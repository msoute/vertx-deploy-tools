package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingAsync;
import com.amazonaws.services.autoscaling.AmazonAutoScalingAsyncClientBuilder;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

import static nl.jpoint.vertx.mod.deploy.util.LogConstants.ERROR_EXECUTING_REQUEST;
import static rx.Observable.just;

public class AwsAutoScalingUtil {
    public static final String LATEST_VERSION_TAG = "deploy:latest:version";
    public static final String SCOPE_TAG = "deploy:scope:tst";
    public static final String EXCLUSION_TAG = "deploy:exclusions";
    public static final String PROPERTIES_TAGS = "deploy:classifier:properties";
    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScalingUtil.class);
    private final AmazonAutoScalingAsync asyncClient;
    private final String instanceId;


    public AwsAutoScalingUtil(DeployConfig config) {
        asyncClient = AmazonAutoScalingAsyncClientBuilder.standard().withRegion(config.getAwsRegion()).build();
        instanceId = EC2MetadataUtils.getInstanceId();
    }


    private Optional<AutoScalingInstanceDetails> describeInstance() {
        DescribeAutoScalingInstancesResult result = asyncClient.describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest().withInstanceIds(Collections.singletonList(instanceId)));
        return result.getAutoScalingInstances().stream().filter(a -> a.getInstanceId().equals(instanceId)).findFirst();
    }

    public Observable<AwsState> pollForInstanceState() {
        try {
            return Observable.from(asyncClient.describeAutoScalingInstancesAsync(new DescribeAutoScalingInstancesRequest().withInstanceIds(instanceId)))
                    .flatMap(result -> {
                        Optional<String> optState = result.getAutoScalingInstances().stream().filter(i -> i.getInstanceId().equals(instanceId)).map(AutoScalingInstanceDetails::getLifecycleState).findFirst();
                        return just(optState.map(AwsState::map).orElse(AwsState.UNKNOWN));
                    });
        } catch (AmazonClientException e) {
            LOG.error(ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }
    }

    public Observable<String> listLoadBalancers(final String groupId) {
        try {
            return Observable.from(asyncClient.describeLoadBalancersAsync(new DescribeLoadBalancersRequest().withAutoScalingGroupName(groupId)))
                    .map(result -> result.getLoadBalancers().stream().map(LoadBalancerState::getLoadBalancerName).collect(Collectors.toList()))
                    .flatMap(Observable::from);
        } catch (AmazonClientException e) {
            LOG.error(ERROR_EXECUTING_REQUEST, e);
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
            LOG.error(ERROR_EXECUTING_REQUEST, e);
            return false;
        }
    }

    public boolean exitStandby(final String groupId) {
        try {
            asyncClient.exitStandby(new ExitStandbyRequest().withAutoScalingGroupName(groupId).withInstanceIds(instanceId));
            return true;
        } catch (AmazonClientException e) {
            LOG.error(ERROR_EXECUTING_REQUEST, e);
            return false;
        }
    }

    public Map<String, String> getDeployTags() {
        Map<String, String> tags = new HashMap<>(3);
        Optional<AutoScalingInstanceDetails> details = describeInstance();
        details.ifPresent(autoScalingInstanceDetails -> {
            autoScalingInstanceDetails.getAutoScalingGroupName();
            List<Filter> filters = Collections.singletonList(
                    new Filter().withName("auto-scaling-group").withValues(autoScalingInstanceDetails.getAutoScalingGroupName())
            );
            DescribeTagsResult result = asyncClient.describeTags(new DescribeTagsRequest().withFilters(filters));
            result.getTags().forEach(t -> tags.put(t.getKey(), t.getValue()));
        });
        return tags;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
