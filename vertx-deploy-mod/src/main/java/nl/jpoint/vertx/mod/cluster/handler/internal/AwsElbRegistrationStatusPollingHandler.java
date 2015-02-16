package nl.jpoint.vertx.mod.cluster.handler.internal;

import nl.jpoint.vertx.mod.cluster.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.cluster.aws.AwsException;
import nl.jpoint.vertx.mod.cluster.aws.AwsState;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;


public class AwsElbRegistrationStatusPollingHandler implements Handler<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(AwsElbRegistrationStatusPollingHandler.class);
    private final DeployRequest request;
    private final AwsElbUtil elbUtil;
    private final Vertx vertx;
    private final AwsState state;

    public AwsElbRegistrationStatusPollingHandler(DeployRequest request, AwsElbUtil elbUtil, Vertx vertx, AwsState state) {
        this.request = request;
        this.elbUtil = elbUtil;
        this.vertx = vertx;
        this.state = state;

        LOG.info("[{} - {}]: Waiting for instance {} status on loadbalancer {} to reach {}.", LogConstants.AWS_ELB_REQUEST, request.getId(), elbUtil.forInstanceId(), elbUtil.forLoadbalancer(), state);
    }

    @Override
    public void handle(Long timer) {
        try {
            AwsState currentState = elbUtil.getInstanceState();
            LOG.info("[{} - {}]: Instance {} on loadbalancer {} in state {}", LogConstants.AWS_ELB_REQUEST, request.getId(), elbUtil.forInstanceId(), elbUtil.forLoadbalancer(), currentState.name());
            if (state.equals(currentState)) {
                vertx.cancelTimer(timer);
                vertx.eventBus().send("aws.service.deploy", new JsonObject().putBoolean("success", true)
                        .putString("id", request.getId().toString())
                        .putString("state", state.toString()));
            }
        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error executing de-register", LogConstants.AWS_ELB_REQUEST, request.getId(),e.getMessage());
        }

    }
}
