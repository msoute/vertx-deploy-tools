package nl.jpoint.vertx.mod.cluster.aws.state;

import nl.jpoint.vertx.mod.cluster.aws.AwsContext;
import nl.jpoint.vertx.mod.cluster.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.cluster.command.Command;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class AwsRegisterFactory {
    public static Command<DeployRequest> getInstance(AwsContext context, DeployRequest deployRequest, JsonObject config, Vertx vertx) {
        if (deployRequest.withAws() && !deployRequest.withAutoscaling()) {
            final AwsElbUtil awsElbUtil = new AwsElbUtil(context.getAwsUtil(),
                    context.getRegion(), config.getString("aws.elb.loadbalancer"), config.getString("aws.elb.instanceid"));
            return new AwsElbRegisterInstance(vertx, awsElbUtil);
        }  else if (deployRequest.withAws() && deployRequest.withAutoscaling()) {
            return new AwsAsRegisterInstance(vertx, context);
        }
        return null;
    }
}
