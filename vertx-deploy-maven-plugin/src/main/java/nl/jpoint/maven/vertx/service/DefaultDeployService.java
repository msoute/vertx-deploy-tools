package nl.jpoint.maven.vertx.service;

import nl.jpoint.maven.vertx.executor.DefaultRequestExecutor;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

public class DefaultDeployService extends DeployService {
    private final DeployConfiguration activeConfiguration;
    private final Integer port;
    private final Integer requestTimeout;

    public DefaultDeployService(DeployConfiguration activeConfiguration, final Integer port, final Integer requestTimeout, Log log) {
        super(null, log);
        this.activeConfiguration = activeConfiguration;
        this.port = port;
        this.requestTimeout = requestTimeout;
    }

    public void normalDeploy(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {

        DeployRequest deployRequest = new DeployRequest.Builder()
                .withModules(deployModuleRequests)
                .withArtifacts(deployArtifactRequests)
                .withConfigs(activeConfiguration.isDeployConfig() ? deployConfigRequests : null)
                .withElb(activeConfiguration.useElbStatusCheck())
                .withRestart(activeConfiguration.doRestart())
                .build();
        final DefaultRequestExecutor executor = new DefaultRequestExecutor(getLog(), requestTimeout, port);

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '" + deployArtifactRequests.size() + "' artifacts and '" + deployModuleRequests.size() + "' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");
        getLog().debug("Sending request -> " + deployRequest.toJson(true));

        for (String host : activeConfiguration.getHosts()) {
            getLog().info("Sending deploy request to host : " + host);
            executor.executeRequest(deployRequest, host, false);
        }

    }
}
