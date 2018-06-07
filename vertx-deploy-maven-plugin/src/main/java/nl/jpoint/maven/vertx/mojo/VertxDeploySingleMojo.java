package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.DeployApplicationRequest;
import nl.jpoint.maven.vertx.request.DeployArtifactRequest;
import nl.jpoint.maven.vertx.request.DeployConfigRequest;
import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@Mojo(name = "deploy-single", requiresDependencyResolution = ResolutionScope.RUNTIME)
class VertxDeploySingleMojo extends AbstractDeployMojo {

    @Parameter(property = "deploy.single.type", required = true)
    private String type;
    @Parameter(property = "deploy.single.groupId", required = true)
    private String groupId;
    @Parameter(property = "deploy.single.artifactId", required = true)
    private String artifactId;
    @Parameter(property = "deploy.single.classifier")
    private String classifier;
    @Parameter(property = "deploy.single.version", required = true)
    private String version;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final DeployUtils utils = new DeployUtils(getLog(), project, remoteRepos, repoSystem, repoSession);
        setActiveDeployConfig();

        activeConfiguration.getExclusions().addAll(utils.parseExclusions(exclusions));

        final List<Request> deployModuleRequests = DeployUtils.APPLICATION_TYPE.equals(type) ? createModuleRequest() : Collections.emptyList();
        final List<Request> deployArtifactRequests = DeployUtils.ARTIFACT_TYPE_ZIP.equals(type) || DeployUtils.ARTIFACT_TYPE_GZIP.equals(type) ? createArtifactRequest() : Collections.emptyList();
        final List<Request> deployConfigRequests = DeployUtils.CONFIG_TYPE.equals(type) ? createConfigRequest() : Collections.emptyList();

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '" + deployArtifactRequests.size() + "' artifacts and '" + deployModuleRequests.size() + "' modules");

        deploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
    }

    private List<Request> createModuleRequest() {
        return Collections.singletonList(new DeployApplicationRequest(groupId, artifactId, version, classifier, type, activeConfiguration.doRestart()));
    }

    private List<Request> createArtifactRequest() {
        return Collections.singletonList(new DeployArtifactRequest(groupId, artifactId, version, classifier, type));
    }

    private List<Request> createConfigRequest() {
        return Collections.singletonList(new DeployConfigRequest(groupId, artifactId, version, classifier, type));
    }
}
