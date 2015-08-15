package nl.jpoint.maven.vertx.mojo;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.executor.DefaultRequestExecutor;
import nl.jpoint.maven.vertx.executor.WaitForInstanceRequestExecutor;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import nl.jpoint.maven.vertx.utils.AwsOpsWorksDeployUtils;
import nl.jpoint.maven.vertx.utils.AwsState;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import nl.jpoint.maven.vertx.utils.Ec2Instance;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;

@Mojo(name = "deploy")
class VertxDeployMojo extends AbstractDeployMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        setActiveDeployConfig();

        if (activeConfiguration.isAutoScaling() && activeConfiguration.isOpsworks()) {
            throw new MojoFailureException("ActiveConfiguration " + activeConfiguration.getTarget() + " has both OpsWorks and Autoscaling enabled");
        }

        final DeployUtils utils = new DeployUtils(getLog(), project);

        final List<Request> deployModuleRequests = utils.createDeployModuleList(activeConfiguration, MODULE_CLASSIFIER);
        final List<Request> deployArtifactRequests = utils.createDeploySiteList(activeConfiguration, SITE_CLASSIFIER);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration, CONFIG_TYPE);

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '" + deployArtifactRequests.size() + "' artifacts and '" + deployModuleRequests.size() + "' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");

        if (activeConfiguration.isAutoScaling()) {
            deployWithAutoScaling(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        } else if (activeConfiguration.isOpsworks()) {
            deployWithOpsWorks(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        } else {
            normalDeploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        }
    }

    private void deployWithAutoScaling(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.getAutoScalingGroupId() == null) {
            throw new MojoExecutionException("ActiveConfiguration " + activeConfiguration.getTarget() + " has no autoScalingGroupId set");

        }
        if (credentialsId == null) {
            throw new MojoExecutionException("credentialsId is not set");
        }
        AwsAutoScalingDeployUtils awsDeployUtils = new AwsAutoScalingDeployUtils(credentialsId, settings, region);

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
            final DefaultRequestExecutor executor = new DefaultRequestExecutor(getLog(), requestTimeout, port);
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
            AwsState newState = executor.executeAwsDeployRequest(deployRequest, (activeConfiguration.getAwsPrivateIp() ? instance.getPrivateIp() : instance.getPublicIp()), activeConfiguration.withElb(), ignoreFailure);
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

    private void deployWithOpsWorks(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.getOpsWorksLayerId() == null) {
            throw new MojoFailureException("ActiveConfiguration " + activeConfiguration.getTarget() + " has no opsWorksLayerId set");
        }

        if (credentialsId == null) {
            throw new MojoExecutionException("credentialsId is not set");
        }

        AwsOpsWorksDeployUtils awsOpsWorksDeployUtils = new AwsOpsWorksDeployUtils(credentialsId, settings, region);
        awsOpsWorksDeployUtils.getHostsOpsWorks(getLog(), activeConfiguration);

        DeployRequest deployRequest = new DeployRequest.Builder()
                .withModules(deployModuleRequests)
                .withArtifacts(deployArtifactRequests)
                .withConfigs(deployConfigRequests)
                .withElb(activeConfiguration.withElb())
                .withRestart(activeConfiguration.doRestart())
                .build();

        for (String host : activeConfiguration.getHosts()) {

            final DefaultRequestExecutor executor = new DefaultRequestExecutor(getLog(), requestTimeout, port);
            executor.executeAwsDeployRequest(deployRequest, host, activeConfiguration.withElb(), false);
        }
    }

    private void normalDeploy(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {

        DeployRequest deployRequest = new DeployRequest.Builder()
                .withModules(deployModuleRequests)
                .withArtifacts(deployArtifactRequests)
                .withConfigs(activeConfiguration.isDeployConfig() ? deployConfigRequests : null)
                .withElb(activeConfiguration.withElb())
                .withRestart(activeConfiguration.doRestart())
                .build();


        for (String host : activeConfiguration.getHosts()) {
            final DefaultRequestExecutor executor = new DefaultRequestExecutor(getLog(), requestTimeout, port);
            executor.executeDeployRequest(deployRequest, host);
        }
    }
}
