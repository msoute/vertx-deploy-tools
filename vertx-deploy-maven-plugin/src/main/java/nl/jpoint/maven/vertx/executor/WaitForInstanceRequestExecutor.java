package nl.jpoint.maven.vertx.executor;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import org.apache.maven.plugin.logging.Log;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitForInstanceRequestExecutor {

    private final Log log;
    private final long timeout;
    private Instance newInstance = null;

    public WaitForInstanceRequestExecutor(Log log, Integer requestTimeout) {
        this.timeout = System.currentTimeMillis() + (60000L * requestTimeout);
        this.log = log;
    }

    private Instance findNewInstance(AutoScalingGroup originalGroup, AutoScalingGroup updatedGroup) {
        updatedGroup.getInstances().removeAll(originalGroup.getInstances());
        return updatedGroup.getInstances().isEmpty() ? null : updatedGroup.getInstances().get(0);
    }

    public void executeRequest(final AutoScalingGroup autoScalingGroup, AwsAutoScalingDeployUtils awsDeployUtils, InstanceStatus instanceStatus) {
        final AtomicInteger waitFor = new AtomicInteger(1);
        final AtomicBoolean inService = new AtomicBoolean(false);
        final AtomicBoolean found = new AtomicBoolean(false);


        log.info("Waiting for new instance in asGroup to come in service...");
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        exec.scheduleAtFixedRate(() -> {
            log.debug("existing instances : " + Arrays.toString(autoScalingGroup.getInstances().stream().map(Instance::getInstanceId).toArray()));
            AutoScalingGroup updatedGroup = awsDeployUtils.getAutoScalingGroup();
            log.debug("Updated instances : " + Arrays.toString(updatedGroup.getInstances().stream().map(Instance::getInstanceId).toArray()));

            if (updatedGroup.getInstances().equals(autoScalingGroup.getInstances())) {
                log.info("no new instance found in auto scaling group.");
            }

            if (newInstance == null) {
                newInstance = findNewInstance(autoScalingGroup, updatedGroup);
                if (newInstance != null && !found.get()) {
                    found.set(true);
                    log.info("Found new instance with id " + newInstance.getInstanceId());
                }
            }
            if (newInstance != null && instanceStatus.inService(newInstance)) {
                waitFor.decrementAndGet();
            }
        }, 30, 30, TimeUnit.SECONDS);

        try {
            while (waitFor.intValue() > 0 && System.currentTimeMillis() <= timeout) {
                Thread.sleep(3000);
            }
            log.info("Shutting down executor");
            exec.shutdown();
            log.info("awaiting termination of executor");
            exec.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
            inService.get();
        } catch (Exception t) {
            log.error("Throwable: ", t);
        }
    }

    @FunctionalInterface
    public interface InstanceStatus {
        Boolean inService(Instance newInstance);
    }

}
