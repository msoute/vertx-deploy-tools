package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;

public abstract class AbstractDeployMojo extends AbstractMojo {

    protected DeployConfiguration activeConfiguration;
    @Component
    protected MavenProject project;
    @Parameter
    protected List<DeployConfiguration> deployConfigurations;
    @Parameter(defaultValue = "default", property = "deploy.activeTarget")
    protected String activeTarget;
    @Parameter(defaultValue = "false", property = "deploy.testScope")
    protected Boolean testScope;
}
