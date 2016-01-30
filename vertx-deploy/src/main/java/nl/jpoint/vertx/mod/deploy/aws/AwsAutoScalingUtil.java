package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingAsyncClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Optional;
import java.util.stream.Collectors;

import static rx.Observable.just;

public class AwsAutoScalingUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScalingUtil.class);

    private final AmazonAutoScalingClient asClient;
    private final AmazonAutoScalingAsyncClient asyncClient;

    public AwsAutoScalingUtil(final AwsContext context) {
        asClient = new AmazonAutoScalingClient(context.getCredentials());
        asClient.setRegion(context.getAwsRegion());
        asyncClient = new AmazonAutoScalingAsyncClient(context.getCredentials());
        asyncClient.setRegion(context.getAwsRegion());
    }

    public Observable<AwsState> pollForInstanceState(final String instanceId) throws AwsException {
        try {
            return Observable.from(asyncClient.describeAutoScalingInstancesAsync(new DescribeAutoScalingInstancesRequest().withInstanceIds(instanceId)))
                    .flatMap(result -> {
                        Optional<String> optState = result.getAutoScalingInstances().stream().filter(i -> i.getInstanceId().equals(instanceId)).map(AutoScalingInstanceDetails::getLifecycleState).findFirst();
                        return just(optState.isPresent() ? AwsState.map(optState.get()) : AwsState.UNKNOWN);
                    });
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }
    }

    public Observable<String> listLoadBalancers(final String groupId) throws AwsException {
        try {
            return Observable.from(asyncClient.describeLoadBalancersAsync(new DescribeLoadBalancersRequest().withAutoScalingGroupName(groupId)))
                    .map(result -> result.getLoadBalancers().stream().map(LoadBalancerState::getLoadBalancerName).collect(Collectors.toList()))
                    .flatMap(Observable::from);
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }
    }

    public boolean enterStandby(final String instanceId, final String groupId, boolean decrementDesiredCapacity) {
        try {
            DescribeAutoScalingInstancesResult result = asClient.describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest().withMaxRecords(1).withInstanceIds(instanceId));
            Optional<AutoScalingInstanceDetails> state = result.getAutoScalingInstances()
                    .stream()
                    .filter(asi -> asi.getInstanceId().equals(instanceId)).findFirst();
            state.ifPresent(s -> LOG.trace("enterStandby() instance {} current state : {}", instanceId, s.getLifecycleState()));
            if (state.isPresent() && state.get().getLifecycleState().equalsIgnoreCase(AwsState.STANDBY.name())) {
                return true;
            } else {
                asClient.enterStandby(new EnterStandbyRequest().withAutoScalingGroupName(groupId).withInstanceIds(instanceId).withShouldDecrementDesiredCapacity(decrementDesiredCapacity));
                return true;
            }
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            return false;
        }
    }

    public boolean exitStandby(final String instanceId, final String groupId) {
        try {
            asClient.exitStandby(new ExitStandbyRequest().withAutoScalingGroupName(groupId).withInstanceIds(instanceId));
            return true;
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            return false;
        }
    }
}
