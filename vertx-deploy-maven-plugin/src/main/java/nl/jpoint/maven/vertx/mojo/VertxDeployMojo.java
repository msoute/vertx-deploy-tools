package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.service.AutoScalingDeployService;
import nl.jpoint.maven.vertx.service.DefaultDeployService;
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

        final DeployUtils utils = new DeployUtils(getLog(), artifacts, remoteRepos, repoSystem, repoSession);

        final List<Request> deployModuleRequests = utils.createDeployApplicationList(activeConfiguration);
        final List<Request> deployArtifactRequests = utils.createDeployArtifactList(activeConfiguration);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration);

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '" + deployArtifactRequests.size() + "' artifacts and '" + deployModuleRequests.size() + "' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");

        if (activeConfiguration.useAutoScaling()) {
            AutoScalingDeployService service = new AutoScalingDeployService(activeConfiguration, region, port, requestTimeout, getLog(), project.getProperties());
            service.deployWithAutoScaling(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        } else {
            DefaultDeployService service = new DefaultDeployService(activeConfiguration, port, requestTimeout, getLog());
            service.normalDeploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        }
    }

}
