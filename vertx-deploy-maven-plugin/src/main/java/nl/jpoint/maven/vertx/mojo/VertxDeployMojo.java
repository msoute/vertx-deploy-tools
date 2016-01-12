package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.service.AutoScalingDeployService;
import nl.jpoint.maven.vertx.service.DefaultDeployService;
import nl.jpoint.maven.vertx.service.OpsWorksDeployService;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.List;

@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.RUNTIME)
class VertxDeployMojo extends AbstractDeployMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        setActiveDeployConfig();

        if (activeConfiguration.useAutoScaling() && activeConfiguration.useOpsWorks()) {
            throw new MojoFailureException("ActiveConfiguration " + activeConfiguration.getTarget() + " has both OpsWorks and Autoscaling enabled");
        }

        final DeployUtils utils = new DeployUtils(getLog(), project);

        final List<Request> deployModuleRequests = utils.createDeployModuleList(activeConfiguration);
        final List<Request> deployArtifactRequests = utils.createDeploySiteList(activeConfiguration, SITE_CLASSIFIER);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration, CONFIG_TYPE);

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '" + deployArtifactRequests.size() + "' artifacts and '" + deployModuleRequests.size() + "' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");

        if (activeConfiguration.useAutoScaling()) {
            AutoScalingDeployService service = new AutoScalingDeployService(activeConfiguration, region, port, requestTimeout, getServer(), getLog());
            service.deployWithAutoScaling(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        } else if (activeConfiguration.useOpsWorks()) {
            OpsWorksDeployService service = new OpsWorksDeployService(activeConfiguration, region, port, requestTimeout, getServer(), getLog());
            service.deployWithOpsWorks(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        } else {
            DefaultDeployService service = new DefaultDeployService(activeConfiguration, port, requestTimeout, getLog());
            service.normalDeploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        }
    }

}
