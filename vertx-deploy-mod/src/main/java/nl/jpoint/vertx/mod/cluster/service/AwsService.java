package nl.jpoint.vertx.mod.cluster.service;

import nl.jpoint.vertx.mod.cluster.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.cluster.command.AwsDeRegisterInstance;
import nl.jpoint.vertx.mod.cluster.command.AwsRegisterInstance;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployState;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class AwsService {
    private static final Logger LOG = LoggerFactory.getLogger(AwsService.class);
    private final Vertx vertx;
    private final JsonObject config;
    private final AwsElbUtil awsElbUtil;

    private final Map<String, DeployRequest> runningRequests = new HashMap<>();

    public AwsService(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;

        awsElbUtil = new AwsElbUtil(config.getString("aws.auth.access.key"), config.getString("aws.auth.secret.access.key"),
                config.getString("aws.elb.region"), config.getString("aws.elb.loadbalancer"), config.getString("aws.elb.instanceid"));
    }

    public boolean registerRequest(DeployRequest deployRequest ) {
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

        AwsDeRegisterInstance deRegisterCommand = new AwsDeRegisterInstance(vertx, awsElbUtil);
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
        AwsRegisterInstance registerCommand = new AwsRegisterInstance(vertx, awsElbUtil);
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
