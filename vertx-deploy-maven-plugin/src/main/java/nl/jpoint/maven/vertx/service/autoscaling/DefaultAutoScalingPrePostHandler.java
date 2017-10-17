package nl.jpoint.maven.vertx.service.autoscaling;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.executor.WaitForInstanceRequestExecutor;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import nl.jpoint.maven.vertx.utils.AwsState;
import nl.jpoint.maven.vertx.utils.Ec2Instance;
import nl.jpoint.maven.vertx.utils.deploy.strategy.DeployStateStrategyFactory;
import nl.jpoint.maven.vertx.utils.deploy.strategy.DeployStrategyType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.Comparator;
import java.util.List;


public class DefaultAutoScalingPrePostHandler implements AutoScalingPrePostHandler {

    private final DeployConfiguration activeConfiguration;
    private final AwsAutoScalingDeployUtils awsDeployUtils;
    private final Log log;

    public DefaultAutoScalingPrePostHandler(DeployConfiguration activeConfiguration, AwsAutoScalingDeployUtils awsDeployUtils, Log log) {

        this.activeConfiguration = activeConfiguration;
        this.awsDeployUtils = awsDeployUtils;
        this.log = log;
    }

    public void preDeploy(List<Ec2Instance> instances, AutoScalingGroup asGroup) throws MojoFailureException, MojoExecutionException {
        if (instances.isEmpty()) {
            throw new MojoFailureException("No instances in AS group." + activeConfiguration.getDeployStrategy());
        }

        if (instances.stream().anyMatch(i -> !i.isReachable(activeConfiguration.getAwsPrivateIp(), activeConfiguration.getPort(), log))) {
            log.error("Error connecting to deploy module on some instances");
            throw new MojoExecutionException("Error connecting to deploy module on some instances");
        }

        if ((activeConfiguration.useElbStatusCheck() && instances.stream().noneMatch(i -> i.getElbState() == AwsState.INSERVICE))
                || !activeConfiguration.useElbStatusCheck() && asGroup.getInstances().stream().noneMatch(i -> "InService".equals(i.getLifecycleState()))) {
            activeConfiguration.setDeployStrategy(DeployStrategyType.WHATEVER);
            log.info("No instances inService, using deploy strategy " + DeployStrategyType.WHATEVER);
        }

        if (shouldSpinNewInstance(awsDeployUtils, asGroup)) {
            WaitForInstanceRequestExecutor.InstanceStatus instanceStatus = newInstance -> !asGroup.getLoadBalancerNames().isEmpty() && awsDeployUtils.checkInstanceInServiceOnAllElb(newInstance, asGroup.getLoadBalancerNames());
            awsDeployUtils.setDesiredCapacity(asGroup, asGroup.getDesiredCapacity() + 1);
            WaitForInstanceRequestExecutor waitForDeployedInstanceRequestExecutor = new WaitForInstanceRequestExecutor(log, 10);
            waitForDeployedInstanceRequestExecutor.executeRequest(asGroup, awsDeployUtils, instanceStatus);
            instances = awsDeployUtils.getInstancesForAutoScalingGroup(log, awsDeployUtils.getAutoScalingGroup());
        }

        instances.sort(Comparator.comparingInt(o -> o.getElbState().ordinal()));

        if (instances.isEmpty()) {
            throw new MojoFailureException("No inService instances found in group " + activeConfiguration.getAutoScalingGroupId() + ". Nothing to do here, move along");
        }

        if (!DeployStateStrategyFactory.isDeployable(activeConfiguration, asGroup, instances)) {
            throw new MojoExecutionException("Auto scaling group is not in a deployable state.");
        }

        if (activeConfiguration.isSticky()) {
            asGroup.getLoadBalancerNames().forEach(elbName -> awsDeployUtils.enableStickiness(elbName, activeConfiguration.getStickyPorts()));
        }
    }

    public void postDeploy(AutoScalingGroup asGroup, Integer originalDesiredCapacity) {
        if (DeployStrategyType.KEEP_CAPACITY.equals(activeConfiguration.getDeployStrategy())) {
            awsDeployUtils.setDesiredCapacity(asGroup, originalDesiredCapacity);
        }

        if (activeConfiguration.isSticky()) {
            asGroup.getLoadBalancerNames().forEach(elbName -> awsDeployUtils.disableStickiness(elbName, activeConfiguration.getStickyPorts()));
        }
    }

    public void handleError(AutoScalingGroup asGroup) {
        if (activeConfiguration.isSticky()) {
            asGroup.getLoadBalancerNames().forEach(elbName -> awsDeployUtils.disableStickiness(elbName, activeConfiguration.getStickyPorts()));
        }
    }

    private boolean shouldSpinNewInstance(AwsAutoScalingDeployUtils awsDeployUtils, AutoScalingGroup asGroup) {
        return DeployStrategyType.KEEP_CAPACITY.equals(activeConfiguration.getDeployStrategy()) && awsDeployUtils.shouldAddExtraInstance(asGroup);
    }
}
