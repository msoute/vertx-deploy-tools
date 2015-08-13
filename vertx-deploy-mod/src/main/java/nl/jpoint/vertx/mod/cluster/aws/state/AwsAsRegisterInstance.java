package nl.jpoint.vertx.mod.cluster.aws.state;

import nl.jpoint.vertx.mod.cluster.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.mod.cluster.aws.AwsContext;
import nl.jpoint.vertx.mod.cluster.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.cluster.aws.AwsState;
import nl.jpoint.vertx.mod.cluster.command.Command;
import nl.jpoint.vertx.mod.cluster.handler.internal.AwsAsRegistrationStatusPollingHandler;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;


public class AwsAsRegisterInstance implements Command<DeployRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbRegisterInstance.class);
    private final Vertx vertx;
    private final Integer maxDuration;
    private final AwsAutoScalingUtil awsAsUtil;
    private final AwsElbUtil awsElbUtil;

    protected AwsAsRegisterInstance(final Vertx vertx, final AwsContext awsContext, final Integer maxDuration) {
        this.vertx = vertx;
        this.maxDuration = maxDuration;
        this.awsAsUtil = new AwsAutoScalingUtil(awsContext);
        this.awsElbUtil = new AwsElbUtil(awsContext,"eu-west-1");
    }

    @Override
    public JsonObject execute(DeployRequest request) {
        if (!awsAsUtil.exitStandby(request.getInstanceId(), request.getAutoScalingGroup())) {
            LOG.error("[{} - {}]: InstanceId {} failed to exit standby in auto scaling group {}", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), request.getAutoScalingGroup());
            return new JsonObject().putBoolean("success", false);
        }

        LOG.info("[{} - {}]: Starting instance status poller for instance id {} in auto scaling group {}", LogConstants.AWS_AS_REQUEST, request.getId(), request.getInstanceId(), request.getAutoScalingGroup());
        vertx.setPeriodic(10000L, new AwsAsRegistrationStatusPollingHandler(request, awsAsUtil, awsElbUtil, vertx, AwsState.INSERVICE, maxDuration));
        
        return new JsonObject().putBoolean("success", true);
    }
}
