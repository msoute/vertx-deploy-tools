package nl.jpoint.vertx.mod.deploy.handler.internal;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.deploy.aws.AwsException;
import nl.jpoint.vertx.mod.deploy.aws.AwsState;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
                vertx.eventBus().send("aws.service.deploy", new JsonObject()
                        .put("success", true)
                        .put("id", request.getId().toString())
                        .put("state", state.toString()));
            }
        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error executing de-register", LogConstants.AWS_ELB_REQUEST, request.getId(), e.getMessage());
        }

    }
}
