package nl.jpoint.vertx.mod.cluster.handler.internal;

import nl.jpoint.vertx.mod.cluster.aws.AwsAutoScalingUtil;
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

import java.util.List;


public class AwsAsRegistrationStatusPollingHandler implements Handler<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(AwsAsRegistrationStatusPollingHandler.class);
    private static final long ONE_SECOND = 1000l;
    private static final int DEFAULT_TIMEOUT_MINUTES = 4;
    private final DeployRequest request;
    private final AwsAutoScalingUtil asUtil;
    private final AwsElbUtil elbUtil;
    private final Vertx vertx;
    private final AwsState state;
    private final long timeout;

    private List<String> loadbalancers = null;

    public AwsAsRegistrationStatusPollingHandler(final DeployRequest request, final AwsAutoScalingUtil asUtil, final AwsElbUtil elbUtil, final Vertx vertx, final AwsState state, final Integer maxDuration) {
        this.request = request;
        this.asUtil = asUtil;
        this.elbUtil = elbUtil;
        this.vertx = vertx;
        this.state = state;
        this.timeout = System.currentTimeMillis() + (ONE_SECOND * DEFAULT_TIMEOUT_MINUTES);

        LOG.info("[{} - {}]: Waiting for instance {} status in auto scaling group {} to reach {}.", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), request.getAutoScalingGroup(), state);
    }

    public AwsAsRegistrationStatusPollingHandler(DeployRequest request, AwsAutoScalingUtil awsAsUtil, Vertx vertx, AwsState standby, final Integer maxDuration) {
        this(request, awsAsUtil, null, vertx, standby, maxDuration);
    }

    @Override
    public void handle(Long timer) {
        try {
            AwsState currentState = asUtil.getInstanceState(request.getInstanceId(), request.getAutoScalingGroup());
            LOG.info("[{} - {}]: Instance {} in auto scaling group {} in state {}", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), request.getAutoScalingGroup(), currentState.name());
            if (state.equals(currentState) && checkElbInService()) {
                vertx.cancelTimer(timer);
                vertx.eventBus().send("aws.service.deploy", new JsonObject().putBoolean("success", true)
                        .putString("id", request.getId().toString())
                        .putString("state", state.toString()));
            } else if (System.currentTimeMillis() > timeout) {
                LOG.error("[{} - {}]: Error executing de-register, timeout while waiting for instance to reach {} ", LogConstants.AWS_AS_REQUEST, request.getId(), state.name());
                vertx.cancelTimer(timer);
                vertx.eventBus().send("aws.service.deploy", new JsonObject().putBoolean("success", false)
                        .putString("id", request.getId().toString())
                        .putString("state", state.toString()));
            }
        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error executing de-register", LogConstants.AWS_AS_REQUEST, request.getId(), e.getMessage());
        }

    }

    private boolean checkElbInService() {
        if (AwsState.INSERVICE.equals(state) && elbUtil != null) {
            if (loadbalancers == null) {
                try {
                    loadbalancers = asUtil.listLoadBalancers(request.getAutoScalingGroup());

                } catch (AwsException e) {
                    LOG.error("[{} - {}]: Error executing list elb in auto scaling group request", LogConstants.AWS_AS_REQUEST, request.getId(), e.getMessage());
                }
            }

            for (String loadbalancer : loadbalancers) {
                try {
                    LOG.info("[{} - {}]: Checking Instance {} state on elb {}", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), loadbalancer);
                    AwsState currentElbState = elbUtil.getInstanceState(request.getInstanceId(), loadbalancer);
                    LOG.info("[{} - {}]: Instance {} on elb {} in state {}", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), loadbalancer, currentElbState.name());
                    if (!AwsState.INSERVICE.equals(currentElbState)) {
                        return false;
                    }
                } catch (AwsException e) {
                    LOG.error("[{} - {}]: Error executing list elb state for instance {} on elb {} with error", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), loadbalancer, e.getMessage());
                    return false;
                }
            }
            LOG.info("[{} - {}]: Instance {}  in service on all elb's {}.", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId());
            return true;
        }
        return true;
    }
}
