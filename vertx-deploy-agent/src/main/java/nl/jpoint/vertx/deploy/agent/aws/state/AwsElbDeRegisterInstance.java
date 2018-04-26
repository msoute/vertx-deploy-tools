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

public class AwsElbDeRegisterInstance implements Command<DeployRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbDeRegisterInstance.class);
    private final AwsElbUtil awsElbUtil;
    private final AwsAutoScalingUtil awsAsUtil;
    private final AwsPollingElbStateObservable poller;

    public AwsElbDeRegisterInstance(io.vertx.core.Vertx vertx, DeployConfig config) {
        this.awsElbUtil = new AwsElbUtil(config);
        this.awsAsUtil = new AwsAutoScalingUtil(config);
        this.poller = new AwsPollingElbStateObservable(vertx, "fakeId", awsElbUtil, LocalDateTime.now().plusMinutes(config.getAwsMaxRegistrationDuration()), s -> true, AwsState.NOTREGISTERED, AwsState.OUTOFSERVICE);
    }

    @Override
    public Observable<DeployRequest> executeAsync(DeployRequest request) {
        try {
            return awsAsUtil.listLoadBalancers(request.getAutoScalingGroup())
                    .flatMap(awsElbUtil::deRegisterInstanceFromLoadbalancer)
                    .flatMap(elb -> poller.poll(request, elb));
        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error while executing request to AWS -> {}", LogConstants.AWS_ELB_REQUEST, request.getId(), e.getMessage(), e);
            throw new IllegalStateException();
        }
    }
}

