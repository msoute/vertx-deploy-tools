package nl.jpoint.vertx.mod.deploy.aws.state;

import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.mod.deploy.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.deploy.aws.AwsException;
import nl.jpoint.vertx.mod.deploy.aws.AwsState;
import nl.jpoint.vertx.mod.deploy.command.Command;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.LocalDateTime;
import java.util.function.Function;

public class AwsElbRegisterInstance implements Command<DeployRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbRegisterInstance.class);
    private final AwsElbUtil awsElbUtil;
    private final AwsAutoScalingUtil awsAsUtil;
    private final AwsPollingElbStateObservable poller;

    public AwsElbRegisterInstance(io.vertx.core.Vertx vertx, String deployId, DeployConfig config, Function<String, Boolean> requestStillActive) {
        this.awsElbUtil = new AwsElbUtil(config);
        this.awsAsUtil = new AwsAutoScalingUtil(config);
        this.poller = new AwsPollingElbStateObservable(vertx, deployId, awsElbUtil, LocalDateTime.now().plusMinutes(config.getAwsMaxRegistrationDuration()), requestStillActive, AwsState.INSERVICE);
    }

    public Observable<DeployRequest> executeAsync(DeployRequest request) {
        try {
            return awsAsUtil.listLoadBalancers(request.getAutoScalingGroup())
                    .flatMap(awsElbUtil::registerInstanceWithLoadBalancer)
                    .flatMap(elb -> poller.poll(request, elb));
        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error while executing request to AWS -> {}", LogConstants.AWS_ELB_REQUEST, request.getId(), e.getMessage());
            throw new IllegalStateException();
        }
    }
}
