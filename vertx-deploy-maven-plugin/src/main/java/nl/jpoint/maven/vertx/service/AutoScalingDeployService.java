package nl.jpoint.maven.vertx.service;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import nl.jpoint.maven.vertx.executor.AwsRequestExecutor;
import nl.jpoint.maven.vertx.executor.RequestExecutor;
import nl.jpoint.maven.vertx.executor.WaitForInstanceRequestExecutor;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
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
import java.util.Properties;
import java.util.stream.Collectors;

public class AutoScalingDeployService extends DeployService {

    private final DeployConfiguration activeConfiguration;
    private final String region;
    private final Integer port;
    private final Integer requestTimeout;
    private final Properties properties;

    public AutoScalingDeployService(DeployConfiguration activeConfiguration, final String region, final Integer port, final Integer requestTimeout, final Log log, Properties properties) throws MojoExecutionException {
        super(log);
        this.activeConfiguration = activeConfiguration;
        this.region = region;
        this.port = port;
        this.requestTimeout = requestTimeout;
        this.properties = properties;
    }

    public void deployWithAutoScaling(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.getAutoScalingGroupId() == null) {
            throw new MojoExecutionException("ActiveConfiguration " + activeConfiguration.getTarget() + " has no autoScalingGroupId set");
        }

        getLog().info("Deploy with strategy : " + activeConfiguration.getDeployStrategy().name());

        AwsAutoScalingDeployUtils awsDeployUtils = new AwsAutoScalingDeployUtils(region, activeConfiguration, getLog());

        AutoScalingGroup asGroup = awsDeployUtils.getAutoScalingGroup();

        if (asGroup == null) {
            throw new MojoFailureException("Invalid auto-scaling group");
        }

        final int originalDesiredCapacity = asGroup.getDesiredCapacity();
        List<Ec2Instance> instances = awsDeployUtils.getInstancesForAutoScalingGroup(getLog(), asGroup);

        if (instances.stream().anyMatch(i -> !i.isReachable(activeConfiguration.getAwsPrivateIp(), port, getLog()))) {
            getLog().error("Error connecting to deploy module on some instances");
            throw new MojoExecutionException("Error connecting to deploy module on some instances");
        }

        if ((activeConfiguration.useElbStatusCheck() && instances.stream().noneMatch(i -> i.getElbState() == AwsState.INSERVICE))
                || !activeConfiguration.useElbStatusCheck() && asGroup.getInstances().stream().noneMatch(i -> "InService".equals(i.getLifecycleState()))) {
            getLog().info("No instances inService, using deploy strategy WHATEVER");
            activeConfiguration.setDeployStrategy(DeployStrategyType.WHATEVER);
        }

        if (DeployStrategyType.KEEP_CAPACITY.equals(activeConfiguration.getDeployStrategy()) && awsDeployUtils.shouldAddExtraInstance(asGroup)) {
            getLog().info("Adding extra instance");
            // we need to add an extra instance and wait for it to come online
            awsDeployUtils.setDesiredCapacity(asGroup, asGroup.getDesiredCapacity() + 1);
            WaitForInstanceRequestExecutor waitForInstanceRequestExecutor = new WaitForInstanceRequestExecutor(getLog(), 10);
            waitForInstanceRequestExecutor.executeRequest(asGroup, awsDeployUtils);
            // update the auto scaling group
            asGroup = awsDeployUtils.getAutoScalingGroup();
            instances = awsDeployUtils.getInstancesForAutoScalingGroup(getLog(), asGroup);
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

        awsDeployUtils.suspendScheduledActions();

        instances = checkInstances(awsDeployUtils, asGroup, instances);

        Integer originalMinSize = asGroup.getMinSize();

        if (asGroup.getMinSize() >= asGroup.getDesiredCapacity()) {
            awsDeployUtils.setMinimalCapacity(asGroup.getDesiredCapacity() <= 0 ? 0 : asGroup.getDesiredCapacity() - 1);
        }

        for (Ec2Instance instance : instances) {
            awsDeployUtils.updateInstanceState(instance, asGroup.getLoadBalancerNames());
            if (!DeployStateStrategyFactory.isDeployable(activeConfiguration, asGroup, instances)) {
                awsDeployUtils.resumeScheduledActions();
                if (activeConfiguration.isSticky()) {
                    asGroup.getLoadBalancerNames().forEach(elbName -> awsDeployUtils.disableStickiness(elbName, activeConfiguration.getStickyPorts()));
                }
                throw new MojoExecutionException("auto scaling group is not in a deployable state.");
            }

            final RequestExecutor executor = new AwsRequestExecutor(getLog(), requestTimeout, port, activeConfiguration.getAuthToken());

            DeployRequest deployRequest = new DeployRequest.Builder()
                    .withModules(deployModuleRequests)
                    .withArtifacts(deployArtifactRequests)
                    .withConfigs(activeConfiguration.isDeployConfig() ? deployConfigRequests : null)
                    .withElb(activeConfiguration.useElbStatusCheck())
                    .withAutoScalingGroup(activeConfiguration.getAutoScalingGroupId())
                    .withDecrementDesiredCapacity(activeConfiguration.isDecrementDesiredCapacity())
                    .withRestart(activeConfiguration.doRestart())
                    .withTestScope(activeConfiguration.isTestScope())
                    .build();
            getLog().debug("Sending deploy request  -> " + deployRequest.toJson(true));
            getLog().info("Sending deploy request to instance with id " + instance.getInstanceId() + " state " + instance.getElbState().name() + " and public IP " + instance.getPublicIp());

            try {
                AwsState newState = executor.executeRequest(deployRequest, activeConfiguration.getAwsPrivateIp() ? instance.getPrivateIp() : instance.getPublicIp(), activeConfiguration.getDeployStrategy().ordinal() > 1);
                getLog().info("Updates state for instance " + instance.getInstanceId() + " to " + newState.name());
                instance.updateState(newState);
                awsDeployUtils.setDeployMetadataTags(activeConfiguration.getProjectVersion(), properties);
            } catch (MojoExecutionException | MojoFailureException e) {
                getLog().error("Error during deploy. Resuming auto scaling processes.", e);
                awsDeployUtils.updateInstanceState(instance, asGroup.getLoadBalancerNames());
                if (!DeployStateStrategyFactory.isDeployableOnError(activeConfiguration, asGroup, instances)) {
                    awsDeployUtils.resumeScheduledActions();
                    if (activeConfiguration.isSticky()) {
                        asGroup.getLoadBalancerNames().forEach(elbName -> awsDeployUtils.disableStickiness(elbName, activeConfiguration.getStickyPorts()));
                    }
                    throw new MojoExecutionException("auto scaling group is not in a deployable state.");
                }
            }
        }
        awsDeployUtils.setMinimalCapacity(originalMinSize);

        if (DeployStrategyType.KEEP_CAPACITY.equals(activeConfiguration.getDeployStrategy())) {
            awsDeployUtils.setDesiredCapacity(asGroup, originalDesiredCapacity);
        }

        if (activeConfiguration.isSticky()) {
            asGroup.getLoadBalancerNames().forEach(elbName -> awsDeployUtils.disableStickiness(elbName, activeConfiguration.getStickyPorts()));
        }

        awsDeployUtils.resumeScheduledActions();
    }


    private List<Ec2Instance> checkInstances(AwsAutoScalingDeployUtils awsDeployUtils, AutoScalingGroup asGroup, List<Ec2Instance> instances) {
        List<String> removedInstances = asGroup.getInstances().stream()
                .filter(i -> i.getLifecycleState().equalsIgnoreCase(AwsState.STANDBY.name()))
                .map(Instance::getInstanceId)
                .filter(awsDeployUtils::checkEc2Instance)
                .collect(Collectors.toList());

        if (removedInstances != null && removedInstances.isEmpty()) {
            instances = instances.stream()
                    .filter(i -> !removedInstances.contains(i.getInstanceId()))
                    .collect(Collectors.toList());
        }
        return instances;
    }
}
