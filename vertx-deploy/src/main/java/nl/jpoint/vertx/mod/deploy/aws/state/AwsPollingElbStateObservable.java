package nl.jpoint.vertx.mod.deploy.aws.state;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.aws.AwsElbUtil;
import nl.jpoint.vertx.mod.deploy.aws.AwsState;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

class AwsPollingElbStateObservable {
    private static final Logger LOG = LoggerFactory.getLogger(AwsPollingElbStateObservable.class);
    private static final Long POLLING_INTERVAL_IN_MS = 3000L;
    private final io.vertx.rxjava.core.Vertx rxVertx;
    private final AwsElbUtil awsElbUtil;
    private final LocalDateTime timeout;
    private final List<AwsState> acceptedStates;
    private final String deployId;
    private final Function<String, Boolean> requestStillActive;

    public AwsPollingElbStateObservable(io.vertx.core.Vertx vertx, String deployId, AwsElbUtil awsElbUtil, LocalDateTime timeout, Function<String, Boolean> requestStillActive, AwsState... acceptedStates) {
        this.deployId = deployId;
        this.requestStillActive = requestStillActive;
        this.rxVertx = new Vertx(vertx);
        this.awsElbUtil = awsElbUtil;
        this.timeout = timeout;
        this.acceptedStates = Arrays.asList(acceptedStates);

    }


    public Observable<DeployRequest> poll(DeployRequest request, String elb) {
        LOG.info("[{} - {}]: Starting instance status poller for instance id {} on loadbalancer {}", LogConstants.AWS_ELB_REQUEST, request.getId(), request.getInstanceId(), elb);
        return doPoll(request, elb);
    }

    private Observable<DeployRequest> doPoll(DeployRequest request, String elb) {
        return rxVertx.timerStream(POLLING_INTERVAL_IN_MS).toObservable()
                .flatMap(x -> awsElbUtil.pollForInstanceState(elb))
                .flatMap(awsState -> {
                    if (!requestStillActive.apply(deployId)) {
                        LOG.error("[{} - {}]: Request canceled, stopping poller {} ", LogConstants.AWS_ELB_REQUEST, request.getId(), awsState.name());
                        throw new IllegalStateException();
                    }
                            if (LocalDateTime.now().isAfter(timeout)) {
                                LOG.error("[{} - {}]: Timeout while waiting for instance to reach {} ", LogConstants.AWS_ELB_REQUEST, request.getId(), awsState.name());
                                throw new IllegalStateException();
                            }
                            LOG.info("[{} - {}]: Instance {} on elb {} in state {}", LogConstants.AWS_ELB_REQUEST, request.getId(), request.getInstanceId(), elb, awsState.name());
                            if (acceptedStates.contains(awsState)) {
                                return Observable.just(request);
                            } else {
                                return doPoll(request, elb);
                            }
                        }
                );
    }

}
