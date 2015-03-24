package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.util.List;

abstract class AbstractDeployMojo extends AbstractMojo {

    static final String SITE_CLASSIFIER = "site";
    static final String MODULE_CLASSIFIER = "mod";
    static final String CONFIG_TYPE = "config";

    DeployConfiguration activeConfiguration;
    @Component
    MavenProject project;
    @Component
    Settings settings;
    @Parameter
    private List<DeployConfiguration> deployConfigurations;
    @Parameter(defaultValue = "default", property = "deploy.activeTarget")
    private String activeTarget;
    @Parameter(defaultValue = "false", property = "deploy.testScope")
    protected Boolean testScope;
    @Parameter
    protected String credentialsId;

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
        return activeConfiguration;
    }
}
