package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.service.AutoScalingDeployService;
import nl.jpoint.maven.vertx.service.DefaultDeployService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;

abstract class AbstractDeployMojo extends AbstractMojo {

    @Parameter(defaultValue = "6789")
    Integer port;
    @Component
    RepositorySystem repoSystem;
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    RepositorySystemSession repoSession;
    @Parameter(defaultValue = "${project.remoteRepositories}", readonly = true, required = true)
    List<RemoteRepository> remoteRepos;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    Settings settings;
    @Parameter(defaultValue = "10", property = "deploy.requestTimeout")
    Integer requestTimeout;
    @Parameter(defaultValue = "eu-west-1")
    String region;
    @Parameter(property = "deploy.exclusions")
    String exclusions;
    private List<DeployConfiguration> deployConfigurations;
    @Parameter(defaultValue = "default", property = "deploy.activeTarget")
    private String activeTarget;
    @Parameter(property = "deploy.credentialsId")
    private String credentialsId;
    @Parameter(required = false, defaultValue = "", property = "deploy.metrics.namespace")
    protected String metricNamespace;
    @Parameter(required = false, defaultValue = "", property = "deploy.metrics.application")
    protected String metricApplication;
    @Parameter(required = false, defaultValue = "", property = "deploy.metrics.environment")
    protected String metricEnvironment;

    DeployConfiguration activeConfiguration;


    void setActiveDeployConfig() throws MojoFailureException {
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
    }

    String projectVersionAsString() {
        return project.getGroupId() + ":" + project.getArtifactId() + ":" + "pom" + ":" + project.getVersion();
    }

    void deploy(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.useAutoScaling()) {
            AutoScalingDeployService service = new AutoScalingDeployService(activeConfiguration, region, port, requestTimeout, getLog(), project.getProperties());
            service.deploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        } else {
            DefaultDeployService service = new DefaultDeployService(activeConfiguration, port, requestTimeout, getLog());
            service.deploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);
        }
    }
}
