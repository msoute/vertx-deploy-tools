package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.DeployModuleRequest;
import nl.jpoint.maven.vertx.utils.RequestExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "deploy-module")
class VertxDeployModuleMojo extends AbstractDeployMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final RequestExecutor executor = new RequestExecutor(getLog(), requestTimeout);

        setActiveDeployConfig();
        DeployModuleRequest request = new DeployModuleRequest(project.getGroupId(),project.getArtifactId(),project.getVersion(), project.getArtifact().getType(), 4, activeConfiguration.doRestart());
        executor.executeSingleDeployRequest(activeConfiguration, request);
    }
}
