package nl.jpoint.vertx.mod.deploy.service;

import nl.jpoint.vertx.mod.deploy.aws.AwsContext;
import nl.jpoint.vertx.mod.deploy.aws.state.AwsDeRegisterFactory;
import nl.jpoint.vertx.mod.deploy.aws.state.AwsRegisterFactory;
import nl.jpoint.vertx.mod.deploy.command.Command;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployState;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class AwsService {
    private static final Logger LOG = LoggerFactory.getLogger(AwsService.class);
    private static final String DEFAULT_REGION = "eu-west-1";
    private final Vertx vertx;
    private final JsonObject config;
    private AwsContext awsContext;

    private final Map<String, DeployRequest> runningRequests = new HashMap<>();

    public AwsService(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;

        awsContext = AwsContext.build(config.getString("aws.auth.access.key"), config.getString("aws.auth.secret.access.key"), config.getString("aws.region", DEFAULT_REGION));
    }

    public boolean registerRequest(DeployRequest deployRequest) {
        if (runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error("[{} - {}]: Request already registered.", LogConstants.AWS_ELB_REQUEST, deployRequest.getId());
            return false;
        }
        runningRequests.put(deployRequest.getId().toString(), deployRequest);
        return true;
    }

    public boolean deRegisterInstance(String buildId) {

        if (!runningRequests.containsKey(buildId)) {
            LOG.error("[{} - {}]: Request not registered.", LogConstants.AWS_ELB_REQUEST, buildId);
            this.failBuild(buildId);
            return false;
        }

        runningRequests.get(buildId).setState(DeployState.WAITING_FOR_DEREGISTER);

        Command<DeployRequest> deRegisterCommand = AwsDeRegisterFactory.getInstance(awsContext, runningRequests.get(buildId), config, vertx);

        JsonObject deRegisterResult = deRegisterCommand.execute(runningRequests.get(buildId));
        if (!deRegisterResult.getBoolean("success")) {
            runningRequests.remove(buildId);
            LOG.error("[{} - {}]: de-register failed. removing request.", LogConstants.AWS_ELB_REQUEST, buildId);
        }
        return deRegisterResult.getBoolean("success");

    }

    public boolean registerInstance(String buildId) {
        if (!runningRequests.containsKey(buildId)) {
            this.failBuild(buildId);
            LOG.error("[{} - {}]: Request not registered.", LogConstants.AWS_ELB_REQUEST, buildId);
            return false;
        }
        Command<DeployRequest> registerCommand = AwsRegisterFactory.getInstance(awsContext, runningRequests.get(buildId), config, vertx);
        registerCommand.execute(runningRequests.get(buildId));
        return false;
    }

    public DeployRequest updateAndGetRequest(DeployState state, String buildId) {

        if (runningRequests.containsKey(buildId)) {
            LOG.info("[{} - {}]: Updating state to {}", LogConstants.AWS_ELB_REQUEST, buildId, state);
            runningRequests.get(buildId).setState(state);
            return runningRequests.get(buildId);
        }
        return null;

    }

    public void failBuild(String buildId) {
        LOG.error("[{} - {}]: Failing build.", LogConstants.AWS_ELB_REQUEST, buildId);
        if (runningRequests.containsKey(buildId)) {
            runningRequests.get(buildId).setState(DeployState.FAILED);
        }
    }

    public DeployState getDeployStatus(String deployId) {

        if (!runningRequests.containsKey(deployId)) {
            return DeployState.UNKNOWN;
        }

        DeployState state = runningRequests.get(deployId).getState();
        if (state.equals(DeployState.SUCCESS) || state.equals(DeployState.FAILED)) {
            runningRequests.remove(deployId);
        }

        return state;
    }
}
