package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.cluster.aws.AwsException;
import nl.jpoint.vertx.mod.cluster.aws.AwsState;
import nl.jpoint.vertx.mod.cluster.handler.internal.AwsRegistrationStatusPollingHandler;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class AwsDeRegisterInstance implements Command<DeployRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(AwsDeRegisterInstance.class);
    private final Vertx vertx;
    private final AwsElbUtil awsElbUtil;

    public AwsDeRegisterInstance(Vertx vertx, AwsElbUtil awsElbUtil) {
        this.vertx = vertx;

        this.awsElbUtil = awsElbUtil;
    }
    @Override
    public JsonObject execute(DeployRequest request) {

        try {
            List<String> instances = awsElbUtil.listLBInstanceMembers();
            if (!instances.contains(awsElbUtil.forInstanceId())) {
                LOG.info("[{} - {}]: Instance {} not registered with loadbalancer {}.", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
                return new JsonObject().putBoolean("success", true);
            }
            if (awsElbUtil.deRegisterInstanceFromLoadbalancer()) {
                LOG.info("[{} - {}]: Failed to de-register Instance {} from loadbalancer {}.", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
                return new JsonObject().putBoolean("success", false);
            }
            LOG.info("[{} - {}]: Starting instance status poller for instance id {} on loadbalancer {}", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
            vertx.setPeriodic(5000L, new AwsRegistrationStatusPollingHandler(request, awsElbUtil, vertx, AwsState.OUTOFSERVICE));

        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error de-register instance {} from loadbalancer {}.", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
            return new JsonObject().putBoolean("success", false);
        }

        return new JsonObject().putBoolean("success", true);
    }
}
