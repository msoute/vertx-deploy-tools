package nl.jpoint.vertx.mod.cluster.aws.state;

import nl.jpoint.vertx.mod.cluster.aws.AwsContext;
import nl.jpoint.vertx.mod.cluster.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.cluster.command.Command;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class AwsDeRegisterFactory {
    public static Command<DeployRequest> getInstance(AwsContext context, DeployRequest deployRequest, JsonObject config, Vertx vertx) {
        if (deployRequest.withElb() && !deployRequest.withAutoScaling()) {
             final AwsElbUtil awsElbUtil = new AwsElbUtil(context.getAwsUtil(),
               context.getRegion(), config.getString("aws.elb.loadbalancer"), config.getString("aws.elb.instanceid"));
            return new AwsElbDeRegisterInstance(vertx, awsElbUtil);
        } else if (deployRequest.withElb() || deployRequest.withAutoScaling()) {
            return new AwsAsDeRegisterInstance(vertx, context, config.getInteger("aws.as.deregister.maxduration", 4));
        }
        throw new IllegalStateException("Unable to create de-registration instance");
    }
}