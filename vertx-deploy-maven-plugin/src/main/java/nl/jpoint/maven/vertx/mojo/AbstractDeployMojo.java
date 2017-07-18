package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.model.ApplicationDependency;
import nl.jpoint.maven.vertx.model.ArtifactDependency;
import nl.jpoint.maven.vertx.model.ConfigDependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.Collections;
import java.util.List;

abstract class AbstractDeployMojo extends AbstractMojo {

    @Parameter(defaultValue = "10", property = "deploy.requestTimeout")
    protected Integer requestTimeout;
    @Parameter(defaultValue = "6789")
    protected Integer port;
    @Component
    RepositorySystem repoSystem;
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    RepositorySystemSession repoSession;
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    List<RemoteRepository> remoteRepos;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;
    @Parameter(defaultValue = "eu-west-1")
    String region;
    @Parameter(property = "deploy.exclusions")
    String exclusions;
    @Parameter()
    List<ConfigDependency> configDependencies = Collections.emptyList();
    @Parameter()
    List<ArtifactDependency> artifactDependencies = Collections.emptyList();
    @Parameter()
    List<ApplicationDependency> applicationDependencies = Collections.emptyList();
    DeployConfiguration activeConfiguration;
    private List<DeployConfiguration> deployConfigurations = Collections.emptyList();
    @Parameter(defaultValue = "default", property = "deploy.activeTarget")
    private String activeTarget;

    DeployConfiguration setActiveDeployConfig() throws MojoFailureException {
        if (deployConfigurations.size() == 1) {
            getLog().info("Found exactly one deploy config to activate.");
            activeConfiguration = deployConfigurations.get(0);
        } else {
            for (DeployConfiguration config : deployConfigurations) {
                if (activeTarget.equals(config.getTarget())) {
                    activeConfiguration = config;
                    break;
                }
            }
        }

        if (activeConfiguration == null) {
            getLog().error("No active deployConfig !");
            throw new MojoFailureException("No active deployConfig !, config should contain at least one config with scope default");
        }


        getLog().info("Deploy config with target " + activeConfiguration.getTarget() + " activated");

        activeConfiguration.withProjectVersion(projectVersionAsString());

        return activeConfiguration;
    }

    String projectVersionAsString() {
        return project.getGroupId() + ":" + project.getArtifactId() + ":" + "pom" + ":" + project.getVersion();
    }
}
