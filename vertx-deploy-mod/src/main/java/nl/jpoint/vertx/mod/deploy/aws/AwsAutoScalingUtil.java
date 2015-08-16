package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.autoscaling.model.DescribeLoadBalancersResult;
import com.amazonaws.services.autoscaling.model.EnterStandbyRequest;
import com.amazonaws.services.autoscaling.model.ExitStandbyRequest;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LoadBalancerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AwsAutoScalingUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScalingUtil.class);

    private static final String IN_SERVICE = "InService";
    private final AmazonAutoScalingClient asClient;

    public AwsAutoScalingUtil(final AwsContext context) {
        asClient = new AmazonAutoScalingClient(context.getCredentials());
        asClient.setRegion(context.getAwsRegion());
    }

    public List<String> listInstancesInGroup(final String groupId) throws AwsException {
        try {
            DescribeAutoScalingGroupsResult result = asClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(groupId));
            return result.getAutoScalingGroups().stream().flatMap(g -> g.getInstances().stream()).filter(i -> IN_SERVICE.equals(i.getLifecycleState())).map(Instance::getInstanceId).collect(Collectors.toList());
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }
    }

    public AwsState getInstanceState(final String instanceId, final String groupId) throws AwsException {
        try {
            DescribeAutoScalingInstancesResult result = asClient.describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest().withInstanceIds(instanceId));
            Optional<String> state = result.getAutoScalingInstances().stream().filter(i -> i.getInstanceId().equals(instanceId)).map(AutoScalingInstanceDetails::getLifecycleState).findFirst();
            return state.isPresent() ? AwsState.map(state.get()) : AwsState.UNKNOWN;
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }
    }

    public List<String> listLoadBalancers(final String groupId) throws AwsException {
        try {
            DescribeLoadBalancersResult result = asClient.describeLoadBalancers(new DescribeLoadBalancersRequest().withAutoScalingGroupName(groupId));
            return result.getLoadBalancers().stream().map(LoadBalancerState::getLoadBalancerName).collect(Collectors.toList());
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }
    }

    public boolean enterStandby(final String instanceId, final String groupId, boolean decrementDesiredCapacity) {
        try {
            asClient.enterStandby(new EnterStandbyRequest().withAutoScalingGroupName(groupId).withInstanceIds(instanceId).withShouldDecrementDesiredCapacity(decrementDesiredCapacity));
            return true;
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
