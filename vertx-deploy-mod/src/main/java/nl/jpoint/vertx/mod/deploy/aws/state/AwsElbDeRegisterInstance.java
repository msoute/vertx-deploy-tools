package nl.jpoint.vertx.mod.deploy.aws.state;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.deploy.aws.AwsException;
import nl.jpoint.vertx.mod.deploy.aws.AwsState;
import nl.jpoint.vertx.mod.deploy.command.Command;
import nl.jpoint.vertx.mod.deploy.handler.internal.AwsElbRegistrationStatusPollingHandler;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AwsElbDeRegisterInstance implements Command<DeployRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbDeRegisterInstance.class);
    private final Vertx vertx;
    private final AwsElbUtil awsElbUtil;

    protected AwsElbDeRegisterInstance(Vertx vertx, AwsElbUtil awsElbUtil) {
        this.vertx = vertx;

        this.awsElbUtil = awsElbUtil;
    }

    @Override
    public JsonObject execute(DeployRequest request) {

        try {
            List<String> instances = awsElbUtil.listLBInstanceMembers();

            if (!instances.contains(awsElbUtil.forInstanceId())) {
                LOG.info("[{} - {}]: Instance {} not registered with loadbalancer {}.", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
                vertx.eventBus().send("aws.service.deploy", new JsonObject().put("success", true)
                        .put("id", request.getId().toString())
                        .put("state", AwsState.NOTREGISTERED.name()));
                return new JsonObject().put("success", true);
            }
            if (awsElbUtil.deRegisterInstanceFromLoadbalancer()) {
                LOG.info("[{} - {}]: Failed to de-register Instance {} from loadbalancer {}.", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
                return new JsonObject().put("success", false);
            }
            LOG.info("[{} - {}]: Starting instance status poller for instance id {} on loadbalancer {}", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
            vertx.setPeriodic(3000L, new AwsElbRegistrationStatusPollingHandler(request, awsElbUtil, vertx, AwsState.OUTOFSERVICE));

        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error de-register instance {} from loadbalancer {}.", LogConstants.AWS_ELB_REQUEST, request.getId(), awsElbUtil.forInstanceId(), awsElbUtil.forLoadbalancer());
            return new JsonObject().put("success", false);
        }

        return new JsonObject().put("success", true);
    }
}
