package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.DeployArtifactRequest;
import nl.jpoint.maven.vertx.utils.RequestExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "deploy-artifact")
class VertxDeployArtifactMojo extends AbstractDeployMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final RequestExecutor executor = new RequestExecutor(getLog());

        setActiveDeployConfig();

        DeployArtifactRequest request = new DeployArtifactRequest(project.getArtifact().getGroupId(),
                project.getArtifact().getArtifactId(),project.getArtifact().getVersion(),
                project.getArtifact().getClassifier(), project.getArtifact().getType(), activeConfiguration.getContext());

        executor.executeSingleDeployRequest(activeConfiguration, request);
    }

}
