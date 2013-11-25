package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;

public abstract class AbstractDeployMojo extends AbstractMojo {

    protected static final String SITE_CLASSIFIER = "site";
    protected static final String MODULE_CLASSIFIER = "mod";

    protected DeployConfiguration activeConfiguration;
    @Component
    protected MavenProject project;
    @Parameter
    protected List<DeployConfiguration> deployConfigurations;
    @Parameter(defaultValue = "default", property = "deploy.activeTarget")
    protected String activeTarget;
    @Parameter(defaultValue = "false", property = "deploy.testScope")
    protected Boolean testScope;


    protected DeployConfiguration setActiveDeployConfig() throws MojoFailureException {
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
        return activeConfiguration;
    }
}
