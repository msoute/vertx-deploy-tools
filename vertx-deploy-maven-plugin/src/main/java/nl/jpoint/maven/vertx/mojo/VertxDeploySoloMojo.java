package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.service.DefaultDeployService;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Mojo(name = "deploy-direct")
public class VertxDeploySoloMojo extends AbstractDeployMojo {

    @Parameter(property = "deploy.remoteIp", required = true)
    private String remoteIp;
    @Parameter(property = "deploy.stubbed", defaultValue = "true")
    private Boolean stubbed;
    @Parameter(property = "deploy.withConfig", defaultValue = "false")
    private Boolean withConfig;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        DeployConfiguration configuration = new DeployConfiguration();
        configuration.getHosts().add(remoteIp);
        configuration.setTestScope(stubbed);
        configuration.setWithConfig(withConfig);

        super.activeConfiguration = configuration;
        final DeployUtils utils = new DeployUtils(getLog(), project);

        final List<Request> deployModuleRequests = utils.createDeployModuleList(activeConfiguration, MODULE_CLASSIFIER);
        final List<Request> deployArtifactRequests = utils.createDeploySiteList(activeConfiguration, SITE_CLASSIFIER);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration, CONFIG_TYPE);

        DefaultDeployService service = new DefaultDeployService(activeConfiguration, port, requestTimeout, getLog());
        service.normalDeploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);

    }
}
