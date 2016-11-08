package nl.jpoint.maven.vertx.service;

import nl.jpoint.maven.vertx.executor.AwsRequestExecutor;
import nl.jpoint.maven.vertx.executor.RequestExecutor;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.utils.AwsOpsWorksDeployUtils;
import nl.jpoint.maven.vertx.utils.InstanceUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

public class OpsWorksDeployService extends DeployService {
    private final DeployConfiguration activeConfiguration;
    private final String region;
    private final Integer port;
    private final Integer requestTimeout;

    public OpsWorksDeployService(final DeployConfiguration activeConfiguration, final String region, final Integer port, final Integer requestTimeout, Log log) {
        super(log);
        this.activeConfiguration = activeConfiguration;
        this.region = region;
        this.port = port;
        this.requestTimeout = requestTimeout;
    }

    public void deployWithOpsWorks(List<Request> deployModuleRequests, List<Request> deployArtifactRequests, List<Request> deployConfigRequests) throws MojoFailureException, MojoExecutionException {
        if (activeConfiguration.getOpsWorksLayerId() == null) {
            throw new MojoFailureException("ActiveConfiguration " + activeConfiguration.getTarget() + " has no opsWorksLayerId set");
        }

        AwsOpsWorksDeployUtils awsOpsWorksDeployUtils = new AwsOpsWorksDeployUtils(region);
        awsOpsWorksDeployUtils.getHostsOpsWorks(getLog(), activeConfiguration);

        DeployRequest deployRequest = new DeployRequest.Builder()
                .withModules(deployModuleRequests)
                .withArtifacts(deployArtifactRequests)
                .withConfigs(deployConfigRequests)
                .withElb(activeConfiguration.useElbStatusCheck())
                .withRestart(activeConfiguration.doRestart())
                .build();

        if (activeConfiguration.getHosts().stream().anyMatch(host -> !InstanceUtils.isReachable(host, port, host, getLog()))) {
            getLog().error("Error connecting to deploy module on some instances");
            throw new MojoExecutionException("Error connecting to deploy module on some instances");
        }

        for (String host : activeConfiguration.getHosts()) {

            final RequestExecutor executor = new AwsRequestExecutor(getLog(), requestTimeout, port, activeConfiguration.getAuthToken());
            executor.executeRequest(deployRequest, host, false);
        }
    }
}
