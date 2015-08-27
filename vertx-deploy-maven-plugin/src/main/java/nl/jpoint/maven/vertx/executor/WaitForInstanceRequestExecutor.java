package nl.jpoint.maven.vertx.executor;


import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

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

    private void checkState(AtomicInteger atomicInteger, AutoScalingGroup originalGroup, AwsAutoScalingDeployUtils awsDeployUtils) {
        log.info("Waiting for new instance in asGroup to come in service...");
        AutoScalingGroup updatedGroup = awsDeployUtils.getAutoScalingGroup();

        if (updatedGroup.getInstances().equals(originalGroup.getInstances())) {
            log.info("no new instance found in autoscaling group.");
        }
        if (newInstance == null) {
            newInstance = findNewInstance(originalGroup, updatedGroup);
            log.info("Found new instance with id " + newInstance.getInstanceId());
        }

        if (!originalGroup.getLoadBalancerNames().isEmpty() && awsDeployUtils.checkInstanceInServiceOnAllElb(newInstance, originalGroup.getLoadBalancerNames(), log)) {
            atomicInteger.decrementAndGet();
        }
    }

    private Instance findNewInstance(AutoScalingGroup originalGroup, AutoScalingGroup updatedGroup) {
        updatedGroup.getInstances().removeAll(originalGroup.getInstances());
        return updatedGroup.getInstances().get(0);
    }

    public boolean executeRequest(final AutoScalingGroup autoScalingGroup, AwsAutoScalingDeployUtils awsDeployUtils) throws MojoExecutionException {
        final AtomicInteger waitFor = new AtomicInteger(1);
        final AtomicBoolean inService = new AtomicBoolean(false);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() ->
                this.checkState(waitFor, autoScalingGroup, awsDeployUtils), 30, 60, TimeUnit.SECONDS);

        try {
            while (waitFor.intValue() > 0 && System.currentTimeMillis() <= timeout) {
                Thread.sleep(30000);
            }
            log.info("Shutting down executor");
            exec.shutdown();
            log.info("awaiting termination of executor");
            exec.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            inService.get();
        }

        return inService.get();
    }

}
