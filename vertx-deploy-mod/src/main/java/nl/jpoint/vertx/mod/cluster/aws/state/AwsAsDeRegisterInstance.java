package nl.jpoint.vertx.mod.cluster.aws.state;


import nl.jpoint.vertx.mod.cluster.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.mod.cluster.aws.AwsContext;
import nl.jpoint.vertx.mod.cluster.aws.AwsState;
import nl.jpoint.vertx.mod.cluster.command.Command;
import nl.jpoint.vertx.mod.cluster.handler.internal.AwsAsRegistrationStatusPollingHandler;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class AwsAsDeRegisterInstance implements Command<DeployRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(AwsAsDeRegisterInstance.class);

    private final Vertx vertx;
    private final AwsAutoScalingUtil awsAsUtil;
    private final Integer maxDuration;

    public AwsAsDeRegisterInstance(final Vertx vertx, final AwsContext context, final Integer maxDuration) {
        this.maxDuration = maxDuration;
        this.awsAsUtil = new AwsAutoScalingUtil(context);
        this.vertx = vertx;
    }

    @Override
    public JsonObject execute(DeployRequest request) {

        if (!awsAsUtil.enterStandby(request.getInstanceId(), request.getAutoScalingGroup(), request.isDecrementDesiredCapacity())) {
            LOG.info("[{} - {}]: Failed to enter standby for Instance {} in auto scaling group {}.", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), request.getAutoScalingGroup());
            return new JsonObject().putBoolean("success", false);
        }
        LOG.info("[{} - {}]: Starting instance status poller for instance id {} in auto scaling group {}", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), request.getAutoScalingGroup());
        vertx.setPeriodic(5000L, new AwsAsRegistrationStatusPollingHandler(request, awsAsUtil, vertx, AwsState.STANDBY, maxDuration));

        return new JsonObject().putBoolean("success", true);
    }
}
