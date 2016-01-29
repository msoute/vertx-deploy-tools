package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.service.DefaultDeployService;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.List;

@Mojo(name = "deploy-direct", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class VertxDeployDirectMojo extends AbstractDeployMojo {

    @Parameter(property = "deploy.remoteIp", required = true)
    private String remoteIp;
    @Parameter(property = "deploy.testScope", defaultValue = "false")
    private Boolean scopeTest;
    @Parameter(property = "deploy.withConfig", defaultValue = "true")
    private Boolean withConfig;
    @Parameter(property = "deploy.allowSnapshots", defaultValue = "false")
    private Boolean allowSnapshots;
    @Parameter(property = "deploy.restart", defaultValue = "false")
    private Boolean restart;
    @Parameter(required = false, defaultValue = "", property = "deploy.auth.token")
    private String authToken;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final DeployUtils utils = new DeployUtils(getLog(), project);

        DeployConfiguration configuration = new DeployConfiguration();
        configuration.getHosts().add(remoteIp);
        configuration.setTestScope(scopeTest);
        configuration.setWithConfig(withConfig);
        configuration.setDeploySnapshots(allowSnapshots);
        configuration.withRestart(restart);
        configuration.getExclusions().addAll(utils.parseExclusions(exclusions));
        configuration.withAuthToken(authToken);
        super.activeConfiguration = configuration;
        
        final List<Request> deployModuleRequests = utils.createDeployModuleList(activeConfiguration);
        final List<Request> deployArtifactRequests = utils.createDeploySiteList(activeConfiguration, SITE_CLASSIFIER);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration, CONFIG_TYPE);

        DefaultDeployService service = new DefaultDeployService(activeConfiguration, port, requestTimeout, getLog());
        service.normalDeploy(deployModuleRequests, deployArtifactRequests, deployConfigRequests);

    }
}
