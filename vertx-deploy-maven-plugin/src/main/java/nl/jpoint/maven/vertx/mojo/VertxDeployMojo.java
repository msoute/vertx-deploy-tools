package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import nl.jpoint.maven.vertx.utils.RequestExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;

@Mojo(name = "deploy")
class VertxDeployMojo extends AbstractDeployMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        setActiveDeployConfig();

        final DeployUtils utils = new DeployUtils(getLog(), project);
        final RequestExecutor executor = new RequestExecutor(getLog());

        final List<Request> deployModuleRequests = utils.createDeployModuleList(activeConfiguration, MODULE_CLASSIFIER, activeConfiguration.doRestart());
        final List<Request> deployArtifactRequests = utils.createDeploySiteList(activeConfiguration, SITE_CLASSIFIER);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration, CONFIG_TYPE);

        DeployRequest deployRequest = new DeployRequest(deployModuleRequests, deployArtifactRequests, deployConfigRequests, activeConfiguration.getAws(), activeConfiguration.doRestart());

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '"+deployArtifactRequests.size()+"' artifacts and '"+deployModuleRequests.size()+"' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");

        executor.executeDeployRequests(activeConfiguration, deployRequest, settings);

    }
}
