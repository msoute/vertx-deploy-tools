package nl.jpoint.vertx.mod.deploy.aws.state;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.mod.deploy.aws.AwsContext;
import nl.jpoint.vertx.mod.deploy.aws.AwsState;
import nl.jpoint.vertx.mod.deploy.command.Command;
import nl.jpoint.vertx.mod.deploy.handler.internal.AwsAsRegistrationStatusPollingHandler;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            return new JsonObject().put("success", false);
        }
        LOG.info("[{} - {}]: Starting instance status poller for instance id {} in auto scaling group {}", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), request.getAutoScalingGroup());
        vertx.setPeriodic(3000L, new AwsAsRegistrationStatusPollingHandler(request, awsAsUtil, vertx, AwsState.STANDBY, maxDuration));

        return new JsonObject().put("success", true);
    }
}
