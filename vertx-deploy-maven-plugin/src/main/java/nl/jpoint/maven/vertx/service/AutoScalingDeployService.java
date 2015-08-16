package nl.jpoint.maven.vertx.service;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.config.DeployConfiguration;
import nl.jpoint.maven.vertx.executor.AwsRequestExecutor;
import nl.jpoint.maven.vertx.executor.RequestExecutor;
import nl.jpoint.maven.vertx.executor.WaitForInstanceRequestExecutor;
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

        AwsAutoScalingDeployUtils awsDeployUtils = new AwsAutoScalingDeployUtils(getServer(), region);

        AutoScalingGroup asGroup = awsDeployUtils.getAutoscalingGroup(activeConfiguration);

        final int originalDesiredCapacity = asGroup.getDesiredCapacity();

        if (activeConfiguration.isKeepCurrentCapacity()) {
            // we need to add an extra instance and wait for it to come online
            awsDeployUtils.setDesiredCapacity(getLog(), asGroup, asGroup.getDesiredCapacity() + 1);
            WaitForInstanceRequestExecutor waitForInstanceRequestExecutor = new WaitForInstanceRequestExecutor(getLog(), 10, activeConfiguration);
            waitForInstanceRequestExecutor.executeRequest(asGroup, awsDeployUtils);
            // update the autoscaling group
            asGroup = awsDeployUtils.getAutoscalingGroup(activeConfiguration);
        }

        List<Ec2Instance> instances = awsDeployUtils.getInstancesForAutoScalingGroup(getLog(), asGroup, activeConfiguration);

        instances.sort((o1, o2) -> o1.getState().ordinal() - o2.getState().ordinal());

        if (instances.isEmpty()) {
            throw new MojoFailureException("No inService instances found in group " + activeConfiguration.getAutoScalingGroupId() + ". Nothing to do here, move along");
        }

        awsDeployUtils.suspendScheduledActions(getLog(), activeConfiguration);

        Integer originalMinSize = asGroup.getMinSize();

        if (asGroup.getMinSize() >= asGroup.getDesiredCapacity()) {
            awsDeployUtils.setMinimalCapacity(getLog(), asGroup.getDesiredCapacity() <= 0 ? 0 : asGroup.getDesiredCapacity() - 1, activeConfiguration);
        }

        for (Ec2Instance instance : instances) {
            final RequestExecutor executor = new AwsRequestExecutor(getLog(), requestTimeout, port);
            boolean awsGroupIsInService = isInService(instances);
            getLog().info("Auto scaling group inService :  " + awsGroupIsInService);
            boolean ignoreFailure = ignoreFailure(awsGroupIsInService, instance, countInServiceInstances(instances));
            getLog().info("Ignoring failure for instance " + instance.getInstanceId() + " : " + ignoreFailure);
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
            AwsState newState = executor.executeRequest(deployRequest, (activeConfiguration.getAwsPrivateIp() ? instance.getPrivateIp() : instance.getPublicIp()), ignoreFailure);
            getLog().info("Updates state for instance " + instance.getInstanceId() + " to " + newState.name());
            instance.updateState(newState);

        }
        awsDeployUtils.setMinimalCapacity(getLog(), originalMinSize, activeConfiguration);

        if (activeConfiguration.isKeepCurrentCapacity()) {
            awsDeployUtils.setDesiredCapacity(getLog(), asGroup, originalDesiredCapacity);
        }
        awsDeployUtils.resumeScheduledActions(getLog(), activeConfiguration);
    }

    private int countInServiceInstances(List<Ec2Instance> instances) {
        int i = 0;
        for (Ec2Instance instance : instances) {
            if (instance.getState().equals(AwsState.INSERVICE)) {
                i++;
            }
        }
        return i;
    }

    private boolean ignoreFailure(boolean groupInService, Ec2Instance ec2Instance, int inServiceInstances) {
        return groupInService && ec2Instance.getState().equals(AwsState.OUTOFSERVICE) && inServiceInstances > 1;
    }

    private boolean isInService(List<Ec2Instance> instances) {
        for (Ec2Instance instance : instances) {
            if (instance.getState().equals(AwsState.INSERVICE)) {
                return true;
            }
        }
        return false;
    }

}
