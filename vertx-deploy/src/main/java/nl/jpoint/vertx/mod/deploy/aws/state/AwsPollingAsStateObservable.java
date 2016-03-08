package nl.jpoint.vertx.mod.deploy.aws.state;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.mod.deploy.aws.AwsState;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

class AwsPollingAsStateObservable {
    private static final Logger LOG = LoggerFactory.getLogger(AwsPollingAsStateObservable.class);
    private static final Long POLLING_INTERVAL_IN_MS = 3000L;
    private final io.vertx.rxjava.core.Vertx rxVertx;
    private final AwsAutoScalingUtil awsAsUtil;
    private final LocalDateTime timeout;
    private final List<AwsState> acceptedStates;

    public AwsPollingAsStateObservable(io.vertx.core.Vertx vertx, AwsAutoScalingUtil awsAsUtil, LocalDateTime timeout, AwsState... acceptedStates) {
        this.rxVertx = new Vertx(vertx);
        this.awsAsUtil = awsAsUtil;
        this.timeout = timeout;
        this.acceptedStates = Arrays.asList(acceptedStates);

    }

    public Observable<DeployRequest> poll(DeployRequest request) {
        LOG.info("[{} - {}]: Setting timeout to {}.", LogConstants.AWS_AS_REQUEST, request.getId(), timeout.toString());
        return doPoll(request);
    }

    private Observable<DeployRequest> doPoll(DeployRequest request) {
        return rxVertx.timerStream(POLLING_INTERVAL_IN_MS).toObservable()
                .flatMap(x -> awsAsUtil.pollForInstanceState())
                .flatMap(awsState -> {
                            if (LocalDateTime.now().isAfter(timeout)) {
                                LOG.error("[{} - {}]: Timeout while waiting for instance to reach {} ", LogConstants.AWS_AS_REQUEST, request.getId(), awsState.name());
                                throw new IllegalStateException();
                            }
                    LOG.info("[{} - {}]: Instance {} in auto scaling group {} in state {}", LogConstants.AWS_AS_REQUEST, request.getId(), awsAsUtil.getInstanceId(), request.getAutoScalingGroup(), awsState.name());
                            if (acceptedStates.contains(awsState)) {
                                return Observable.just(request);
                            } else {
                                return doPoll(request);
                            }
                        }
                );
    }

}
