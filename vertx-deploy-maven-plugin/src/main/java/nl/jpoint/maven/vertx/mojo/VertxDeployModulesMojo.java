package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import nl.jpoint.maven.vertx.utils.RequestExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;

@Mojo(name = "deploy-module")
public class VertxDeployModulesMojo extends AbstractDeployMojo {

    private static final String MODULE_CLASSIFIER = "mod";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final DeployUtils utils = new DeployUtils(getLog(), project);
        final RequestExecutor executor = new RequestExecutor(getLog());

        activeConfiguration = utils.setActiveDeployConfig(deployConfigurations, activeTarget);

        final List<Request> deployModuleRequests = utils.createDeployModuleList(activeConfiguration, MODULE_CLASSIFIER);

        executor.executeDeployRequests(activeConfiguration, deployModuleRequests);
    }
}
