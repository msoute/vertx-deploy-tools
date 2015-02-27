package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.AwsDeployUtils;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import nl.jpoint.maven.vertx.utils.Ec2Instance;
import nl.jpoint.maven.vertx.utils.RequestExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Comparator;
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
            deployWithOpsWorks(deployModuleRequests, deployArtifactRequests, deployConfigRequests );
        } else {
            normalDeploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests );
        }
    }

    private void deployWithAutoScaling(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.getAutoScalingGroupId() == null) {
            throw new MojoExecutionException("ActiveConfiguration " + activeConfiguration.getTarget() + " has no autoScalingGroupId set");
        }
        AwsDeployUtils awsDeployUtils = new AwsDeployUtils(activeConfiguration.getAutoScalingGroupId(), settings);


        List<Ec2Instance> instances = awsDeployUtils.getInstancesForAutoScalingGroup(getLog(), activeConfiguration);

        instances.sort(new Comparator<Ec2Instance>() {
            @Override
            public int compare(Ec2Instance o1, Ec2Instance o2) {
                return o1.getState().ordinal() - o2.getState().ordinal();
            }
        });


        if (instances.isEmpty()) {
            throw new MojoFailureException("No inService instances found in group " + activeConfiguration.getAutoScalingGroupId() + ". Nothing to do here, move along");
        }

        for (Ec2Instance instance : instances) {
            final RequestExecutor executor = new RequestExecutor(getLog());
            DeployRequest deployRequest = new DeployRequest.Builder()
                    .withModules(deployModuleRequests)
                    .withArtifacts(deployArtifactRequests)
                    .withConfigs(deployConfigRequests)
                    .withElb(activeConfiguration.withElb())
                    .withInstanceId(instance.getInstanceId())
                    .withAutoScalingGroup(activeConfiguration.getAutoScalingGroupId())
                    .withRestart(activeConfiguration.doRestart())
                    .build();
            getLog().debug("Sending deploy request  -> " + deployRequest.toJson(true));
            getLog().info("Sending deploy request to instance with id " + instance.getInstanceId() + " state "+ instance.getState().name() + " and public IP " + instance.getPublicIp());
            executor.executeAwsDeployRequest(deployRequest, (activeConfiguration.getAwsPrivateIp() ? instance.getPrivateIp() : instance.getPublicIp()));
        }

    }

    private void deployWithOpsWorks(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.getOpsWorksStackId() == null) {
            throw new MojoFailureException("ActiveConfiguration " + activeConfiguration.getTarget() + " has no opsWorksStackId set");
        }
        AwsDeployUtils awsDeployUtils = new AwsDeployUtils(activeConfiguration.getOpsWorksStackId(), settings);
        awsDeployUtils.getHostsOpsWorks(getLog(), activeConfiguration);

        DeployRequest deployRequest = new DeployRequest.Builder()
                .withModules(deployModuleRequests)
                .withArtifacts(deployArtifactRequests)
                .withConfigs(deployConfigRequests)
                .withElb(activeConfiguration.withElb())
                .withRestart(activeConfiguration.doRestart())
                .build();

        for (String host : activeConfiguration.getHosts()) {

            final RequestExecutor executor = new RequestExecutor(getLog());
            executor.executeAwsDeployRequest(deployRequest, host);
        }
    }

    private void normalDeploy(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {

        DeployRequest deployRequest = new DeployRequest.Builder()
                .withModules(deployModuleRequests)
                .withArtifacts(deployArtifactRequests)
                .withConfigs(deployConfigRequests)
                .withElb(activeConfiguration.withElb())
                .withRestart(activeConfiguration.doRestart())
                .build();


        for (String host : activeConfiguration.getHosts()) {
            final RequestExecutor executor = new RequestExecutor(getLog());
            executor.executeDeployRequest(deployRequest, host);
        }
    }
}
