package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.service.AutoScalingDeployService;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.List;

@Mojo(name = "deploy-single-as", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class VertxDeployAwsAsMojo extends AbstractAwsDeployMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final DeployUtils utils = new DeployUtils(getLog(), remoteRepos, repoSystem, repoSession);

        activeConfiguration = this.createConfiguration();
        activeConfiguration.getExclusions().addAll(utils.parseExclusions(exclusions));
        activeConfiguration.getAutoScalingProperties().addAll(utils.parseProperties(properties));
        final List<Request> deployApplicationRequests = utils.createDeployApplicationList(applicationDependencies, activeConfiguration);
        final List<Request> deployArtifactRequests = utils.createDeployArtifactList(artifactDependencies, activeConfiguration);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(configDependencies, activeConfiguration);

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '" + deployArtifactRequests.size() + "' artifacts and '" + deployApplicationRequests.size() + "' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");

        AutoScalingDeployService service = new AutoScalingDeployService(activeConfiguration, region, port, requestTimeout, getLog(), project.getProperties());
        service.deployWithAutoScaling(deployApplicationRequests, deployArtifactRequests, deployConfigRequests);

    }
}
