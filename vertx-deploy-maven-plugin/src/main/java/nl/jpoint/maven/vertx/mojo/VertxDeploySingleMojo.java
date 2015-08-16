package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.executor.DefaultRequestExecutor;
import nl.jpoint.maven.vertx.request.DeployModuleRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "deploy-single")
class VertxDeploySingleMojo extends AbstractDeployMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final DefaultRequestExecutor executor = new DefaultRequestExecutor(getLog(), requestTimeout, port);
        setActiveDeployConfig();
        // DeployModuleRequest request = new DeployModuleRequest();
        // executor.executeRequest(request, activeConfiguration, false);
    }

    private DeployModuleRequest createModuleRequest() {
        return new DeployModuleRequest(project.getGroupId(), project.getArtifactId(), project.getVersion(), project.getArtifact().getType(), 4, activeConfiguration.doRestart());
    }
}
