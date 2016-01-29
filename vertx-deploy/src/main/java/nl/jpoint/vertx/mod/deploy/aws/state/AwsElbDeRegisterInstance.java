package nl.jpoint.vertx.mod.deploy.aws.state;

import nl.jpoint.vertx.mod.deploy.aws.*;
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

    public AwsElbDeRegisterInstance(io.vertx.core.Vertx vertx, AwsContext awsContext, Integer maxDuration) {
        this.awsElbUtil = new AwsElbUtil(awsContext);
        this.awsAsUtil = new AwsAutoScalingUtil(awsContext);
        this.poller = new AwsPollingElbStateObservable(vertx, awsElbUtil, LocalDateTime.now().plusMinutes(maxDuration), AwsState.NOTREGISTERED, AwsState.OUTOFSERVICE);
    }

    public Observable<DeployRequest> executeAsync(DeployRequest request) {
        try {
            return awsAsUtil.listLoadBalancers(request.getAutoScalingGroup())
                    .flatMap(elb -> awsElbUtil.deRegisterInstanceFromLoadbalancer(request.getInstanceId(), elb))
                    .flatMap(elb -> poller.poll(request, elb));
        } catch (AwsException e) {
            LOG.error("[{} - {}]: Error while executing request to AWS -> {}", LogConstants.AWS_ELB_REQUEST, request.getId(), e.getMessage());
            throw new IllegalStateException();
        }
    }
}

