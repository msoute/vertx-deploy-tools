package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.cluster.aws.AwsException;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class AwsRegisterInstance implements Command {
    private static final Logger LOG = LoggerFactory.getLogger(AwsRegisterInstance.class);
    private final Vertx vertx;
    private final JsonObject config;
    private final AwsElbUtil awsElbUtil;
    private final String loadbalancerName;
    private final String instanceId;
    private final String region;

    public AwsRegisterInstance(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        awsElbUtil = new AwsElbUtil(config.getString("aws.auth.access.key"), config.getString("aws.auth.secret.access.key"));

        loadbalancerName = config.getString("aws.elb.loadbalancer");
        instanceId = config.getString("aws.elb.instanceid");
        region = config.getString("aws.elb.region");

    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        try {
            List<String> instances = awsElbUtil.listLBInstanceMembers(region, loadbalancerName);
            if (instances.contains(instanceId)) {
                LOG.info("[{} - {}]: InstaceId {} is all ready listed as member of loadbalancer {}", LogConstants.AWS_ELB_REQUEST, request.getId(), instanceId, loadbalancerName);
                return new JsonObject().putBoolean("success", false);
            }

            awsElbUtil.registerInstanceFromLoadbalancer(region,loadbalancerName, instanceId);

            if (!request.isAsync()) {
                LOG.info("[{} - {}]: InstaceId {} registered (async) as member of loadbalancer {}. ", LogConstants.AWS_ELB_REQUEST, request.getId(), instanceId, loadbalancerName);
                return new JsonObject().putBoolean("success", false);
            }



        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error while executing request to AWS -> {}", LogConstants.AWS_ELB_REQUEST, request.getId(),e.getMessage());
            return new JsonObject().putBoolean("success", false);
        }
        return null;

    }
}
