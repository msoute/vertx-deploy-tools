package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import nl.jpoint.maven.vertx.utils.RequestExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Mojo(name = "single-deploy")
public class VertxSingleDeployMojo extends AbstractDeployMojo {

    @Parameter(property = "deploy.remoteIp")
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

        boolean doRestart = true;

        super.activeConfiguration = configuration;
        final DeployUtils utils = new DeployUtils(getLog(), project);
        final RequestExecutor executor = new RequestExecutor(getLog());

        final List<Request> deployModuleRequests = utils.createDeployModuleList(activeConfiguration, MODULE_CLASSIFIER);
        final List<Request> deployArtifactRequests = utils.createDeploySiteList(activeConfiguration, SITE_CLASSIFIER);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration, CONFIG_TYPE);

        DeployRequest deployRequest = new DeployRequest.Builder()
                .withModules(deployModuleRequests)
                .withArtifacts(deployArtifactRequests)
                .withConfigs(activeConfiguration.isDeployConfig() ? deployConfigRequests : null)
                .withRestart(doRestart)
                .withElb(false)
                .build();

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '"+deployArtifactRequests.size()+"' artifacts and '"+deployModuleRequests.size()+"' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");
        getLog().debug("Sending request -> " + deployRequest.toJson(true));
        executor.executeDeployRequest(deployRequest, remoteIp);

    }
}
