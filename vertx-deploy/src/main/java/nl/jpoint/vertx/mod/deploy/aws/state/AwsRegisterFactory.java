package nl.jpoint.vertx.mod.deploy.aws.state;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.aws.AwsContext;
import nl.jpoint.vertx.mod.deploy.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.deploy.command.Command;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;

public class AwsRegisterFactory {
    public static Command<DeployRequest> getInstance(AwsContext context, DeployRequest deployRequest, DeployConfig config, Vertx vertx) {
        if (deployRequest.withElb() && !deployRequest.withAutoScaling()) {
            final AwsElbUtil awsElbUtil = new AwsElbUtil(context,config.getAwsLoadbalancerId(), config.getAwsInstanceId());
            return new AwsElbRegisterInstance(vertx, awsElbUtil);
        } else if (deployRequest.withElb() && deployRequest.withAutoScaling()) {
            return new AwsAsRegisterInstance(vertx, context, config.getAwsMaxRegistrationDuration());
        }
        throw new IllegalStateException("Unable to create registration instance");
    }
}
