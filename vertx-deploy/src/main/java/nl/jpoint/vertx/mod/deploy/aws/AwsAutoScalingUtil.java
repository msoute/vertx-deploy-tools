package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingAsyncClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

import static rx.Observable.just;

public class AwsAutoScalingUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScalingUtil.class);

    private final AmazonAutoScalingAsyncClient asyncClient;
    public static final String LATEST_VERSION_TAG = "deploy:latest:version";
    public static final String SCOPE_TAG = "deploy:scope:tst";
    public static final String EXCLUSION_TAG = "deploy:exclusions";
    private final String instanceId;


    public AwsAutoScalingUtil(final AwsContext context) {
        asyncClient = new AmazonAutoScalingAsyncClient();
        asyncClient.setRegion(context.getAwsRegion());
        instanceId = EC2MetadataUtils.getInstanceId();
    }


    public Optional<AutoScalingInstanceDetails> describeInstance() {
        DescribeAutoScalingInstancesResult result = asClient.describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest().withInstanceIds(Collections.singletonList(instanceId)));
        return result.getAutoScalingInstances().stream().filter(a -> a.getInstanceId().equals(instanceId)).findFirst();
    }

    public Observable<AwsState> pollForInstanceState() throws AwsException {
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

    public boolean enterStandby(final String groupId, boolean decrementDesiredCapacity) {
        try {
            DescribeAutoScalingInstancesResult result = asyncClient.describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest().withMaxRecords(1).withInstanceIds(instanceId));
            Optional<AutoScalingInstanceDetails> state = result.getAutoScalingInstances()
                    .stream()
                    .filter(asi -> asi.getInstanceId().equals(instanceId)).findFirst();
            state.ifPresent(s -> LOG.trace("enterStandby() instance {} current state : {}", instanceId, s.getLifecycleState()));
            if (state.isPresent() && state.get().getLifecycleState().equalsIgnoreCase(AwsState.STANDBY.name())) {
                return true;
            } else {
                asyncClient.enterStandby(new EnterStandbyRequest().withAutoScalingGroupName(groupId).withInstanceIds(instanceId).withShouldDecrementDesiredCapacity(decrementDesiredCapacity));
                return true;
            }
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            return false;
        }
    }

    public boolean exitStandby(final String groupId) {
        try {
            asyncClient.exitStandby(new ExitStandbyRequest().withAutoScalingGroupName(groupId).withInstanceIds(instanceId));
            return true;
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            return false;
        }
    }

    public Map<String, String> getDeployTags() {
        Map<String, String> tags = new HashMap<>(3);
        Optional<AutoScalingInstanceDetails> details = describeInstance();
        if (details.isPresent()) {
            details.get().getAutoScalingGroupName();
            List<Filter> filters = Collections.singletonList(
                    new Filter().withName("auto-scaling-group").withValues(details.get().getAutoScalingGroupName())
            );
            DescribeTagsResult result = asClient.describeTags(new DescribeTagsRequest().withFilters(filters));
            result.getTags().stream().forEach(t -> tags.put(t.getKey(), t.getValue()));
        }
        return tags;
    }
}
