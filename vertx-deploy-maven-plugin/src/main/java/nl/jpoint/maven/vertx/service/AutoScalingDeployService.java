package nl.jpoint.maven.vertx.service;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.executor.AwsRequestExecutor;
import nl.jpoint.maven.vertx.executor.RequestExecutor;
import nl.jpoint.maven.vertx.executor.WaitForInstanceRequestExecutor;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import nl.jpoint.maven.vertx.utils.AwsState;
import nl.jpoint.maven.vertx.utils.Ec2Instance;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;

import java.util.List;

public class AutoScalingDeployService extends DeployService {

    private final DeployConfiguration activeConfiguration;
    private final String region;
    private final Integer port;
    private final Integer requestTimeout;

    public AutoScalingDeployService(DeployConfiguration activeConfiguration, final String region, final Integer port, final Integer requestTimeout, final Server server, final Log log) throws MojoExecutionException {
        super(server, log);
        this.activeConfiguration = activeConfiguration;
        this.region = region;
        this.port = port;
        this.requestTimeout = requestTimeout;
    }

    public void deployWithAutoScaling(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.getAutoScalingGroupId() == null) {
            throw new MojoExecutionException("ActiveConfiguration " + activeConfiguration.getTarget() + " has no autoScalingGroupId set");
        }

        AwsAutoScalingDeployUtils awsDeployUtils = new AwsAutoScalingDeployUtils(getServer(), region, activeConfiguration);

        AutoScalingGroup asGroup = awsDeployUtils.getAutoscalingGroup();

        final int originalDesiredCapacity = asGroup.getDesiredCapacity();

        if (activeConfiguration.isKeepCurrentCapacity() && awsDeployUtils.shouldDeployToAutoScalingGroup(asGroup)) {
            // we need to add an extra instance and wait for it to come online
            awsDeployUtils.setDesiredCapacity(getLog(), asGroup, asGroup.getDesiredCapacity() + 1);
            WaitForInstanceRequestExecutor waitForInstanceRequestExecutor = new WaitForInstanceRequestExecutor(getLog(), 10);
            waitForInstanceRequestExecutor.executeRequest(asGroup, awsDeployUtils);
            // update the auto scaling group
            asGroup = awsDeployUtils.getAutoscalingGroup();
            // update the auto scaling group instance list
        }
        List<Ec2Instance> instances = awsDeployUtils.getInstancesForAutoScalingGroup(getLog(), asGroup);

        instances.sort((o1, o2) -> o1.getState().ordinal() - o2.getState().ordinal());

        if (instances.isEmpty()) {
            throw new MojoFailureException("No inService instances found in group " + activeConfiguration.getAutoScalingGroupId() + ". Nothing to do here, move along");
        }

        if (!awsDeployUtils.isDeployable(asGroup, instances)) {
            throw new MojoExecutionException("Autoscaling group is not in a deployable state.");
        }

        awsDeployUtils.suspendScheduledActions(getLog());

        Integer originalMinSize = asGroup.getMinSize();

        if (asGroup.getMinSize() >= asGroup.getDesiredCapacity()) {
            awsDeployUtils.setMinimalCapacity(getLog(), asGroup.getDesiredCapacity() <= 0 ? 0 : asGroup.getDesiredCapacity() - 1);
        }

        for (Ec2Instance instance : instances) {
            awsDeployUtils.updateInstanceState(instance, asGroup.getLoadBalancerNames());
            if (!awsDeployUtils.isDeployable(asGroup, instances)) {
                awsDeployUtils.resumeScheduledActions(getLog());
                throw new MojoExecutionException("Auto-scaling group is not in a deployable state.");
            }

            final RequestExecutor executor = new AwsRequestExecutor(getLog(), requestTimeout, port);

            DeployRequest deployRequest = new DeployRequest.Builder()
                    .withModules(deployModuleRequests)
                    .withArtifacts(deployArtifactRequests)
                    .withConfigs(activeConfiguration.isDeployConfig() ? deployConfigRequests : null)
                    .withElb(activeConfiguration.withElb())
                    .withInstanceId(instance.getInstanceId())
                    .withAutoScalingGroup(activeConfiguration.getAutoScalingGroupId())
                    .withDecrementDesiredCapacity(activeConfiguration.isDecrementDesiredCapacity())
                    .withRestart(activeConfiguration.doRestart())
                    .build();
            getLog().debug("Sending deploy request  -> " + deployRequest.toJson(true));
            getLog().info("Sending deploy request to instance with id " + instance.getInstanceId() + " state " + instance.getState().name() + " and public IP " + instance.getPublicIp());

            try {
                AwsState newState = executor.executeRequest(deployRequest, (activeConfiguration.getAwsPrivateIp() ? instance.getPrivateIp() : instance.getPublicIp()), true);
                getLog().info("Updates state for instance " + instance.getInstanceId() + " to " + newState.name());
                instance.updateState(newState);
            } catch (MojoExecutionException | MojoFailureException e) {
                getLog().error("Error during deploy. Resuming auto scaling processes.", e);
                awsDeployUtils.updateInstanceState(instance, asGroup.getLoadBalancerNames());
                if (!awsDeployUtils.isDeployable(asGroup, instances)) {
                    awsDeployUtils.resumeScheduledActions(getLog());
                    throw new MojoExecutionException("Auto-scaling group is not in a deployable state.");
                }
            }
        }
        awsDeployUtils.setMinimalCapacity(getLog(), originalMinSize);

        if (activeConfiguration.isKeepCurrentCapacity()) {
            awsDeployUtils.setDesiredCapacity(getLog(), asGroup, originalDesiredCapacity);
        }
        awsDeployUtils.resumeScheduledActions(getLog());
    }
}
