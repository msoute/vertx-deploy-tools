package nl.jpoint.maven.vertx.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.*;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AwsAutoScalingDeployUtils {

    private static final String LATEST_REQUEST_TAG = "deploy:latest:version";
    private static final String SCOPE_TAG = "deploy:scope:tst";
    private static final String EXCLUSION_TAG = "deploy:exclusions";
    private static final String PROPERTIES_TAGS = "deploy:classifier:properties";
    private static final String DEPLOY_STICKINESS_POLICY = "deploy-stickiness-policy";
    private static final String AUTO_SCALING_GROUP = "auto-scaling-group";

    private final AmazonAutoScaling awsAsClient;
    private final AmazonElasticLoadBalancing awsElbClient;
    private final AmazonEC2 awsEc2Client;
    private final DeployConfiguration activeConfiguration;
    private final Log log;


    public AwsAutoScalingDeployUtils(String region, DeployConfiguration activeConfiguration, Log log) {
        this.activeConfiguration = activeConfiguration;
        this.log = log;

        awsAsClient = AmazonAutoScalingClientBuilder.standard().withRegion(region).build();
        awsElbClient = AmazonElasticLoadBalancingClientBuilder.standard().withRegion(region).build();
        awsEc2Client = AmazonEC2ClientBuilder.standard().withRegion(region).build();

        activeConfiguration.withAutoScalingGroup(matchAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));

    }

    public AutoScalingGroup getAutoScalingGroup() {

        DescribeAutoScalingGroupsResult result = awsAsClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(activeConfiguration.getAutoScalingGroupId()));

        if (result.getAutoScalingGroups().isEmpty()) {
            log.error("No Autoscaling group found with id : " + activeConfiguration.getAutoScalingGroupId());
            throw new IllegalStateException();
        }
        return result.getAutoScalingGroups().get(0);
    }

    public void suspendScheduledActions() {
        awsAsClient.suspendProcesses(new SuspendProcessesRequest()
                .withScalingProcesses("ScheduledActions", "Terminate", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Should a build fail the processes can be resumed using the AWS CLI.");
        log.info("aws autoscaling resume-processes --auto-scaling-group-name " + activeConfiguration.getAutoScalingGroupId() + " --scaling-processes AZRebalance ReplaceUnhealthy Terminate ScheduledActions AlarmNotification");
        log.info("Suspended auto scaling processes.");
    }

    public void setMinimalCapacity(int cap) {
        log.info("Set minimal capacity for group to " + cap);
        awsAsClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()).withMinSize(cap));
    }

    public void resumeScheduledActions() {
        awsAsClient.resumeProcesses(new ResumeProcessesRequest()
                .withScalingProcesses("ScheduledActions", "Terminate", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Resumed auto scaling processes.");
    }

    public boolean checkInstanceInService(String instanceId) {
        DescribeInstancesResult instancesResult = awsEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        return instancesResult.getReservations().stream()
                .flatMap(r -> r.getInstances().stream())
                .filter(instance -> instance.getInstanceId().equals(instanceId))
                .map(this::toEc2Instance).findFirst().map(ec2Instance -> ec2Instance.isReachable(activeConfiguration.getAwsPrivateIp(), activeConfiguration.getPort(), log)).orElse(false);
    }

    public List<Ec2Instance> getInstancesForAutoScalingGroup(Log log, AutoScalingGroup autoScalingGroup) throws MojoFailureException {
        log.info("retrieving list of instanceId's for auto scaling group with id : " + activeConfiguration.getAutoScalingGroupId());

        activeConfiguration.getHosts().clear();

        log.debug("describing instances in auto scaling group");

        if (autoScalingGroup.getInstances().isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Instance> instanceMap = autoScalingGroup.getInstances().stream().collect(Collectors.toMap(Instance::getInstanceId, Function.identity()));

        try {
            DescribeInstancesResult instancesResult = awsEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(autoScalingGroup.getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList())));
            List<Ec2Instance> ec2Instances = instancesResult.getReservations().stream().flatMap(r -> r.getInstances().stream()).map(this::toEc2Instance).collect(Collectors.toList());
            log.debug("describing elb status");
            autoScalingGroup.getLoadBalancerNames().forEach(elb -> this.updateInstancesStateOnLoadBalancer(elb, ec2Instances));
            ec2Instances.forEach(i -> i.updateAsState(AwsState.map(instanceMap.get(i.getInstanceId()).getLifecycleState())));
            ec2Instances.sort((o1, o2) -> {

                int sComp = o1.getAsState().compareTo(o2.getAsState());

                if (sComp != 0) {
                    return sComp;
                } else {
                    return o1.getElbState().compareTo(o2.getElbState());
                }
            });
            if (activeConfiguration.isIgnoreInStandby()) {
                return ec2Instances.stream().filter(i -> i.getAsState() != AwsState.STANDBY).collect(Collectors.toList());
            }
            return ec2Instances;
        } catch (AmazonClientException e) {
            log.error(e.getMessage(), e);
            throw new MojoFailureException(e.getMessage());
        }

    }

    public void enableStickiness(String loadbalancerName, List<Integer> ports) {
        awsElbClient.createLBCookieStickinessPolicy(new CreateLBCookieStickinessPolicyRequest().withPolicyName(DEPLOY_STICKINESS_POLICY + "-" + loadbalancerName).withLoadBalancerName(loadbalancerName));
        describeMatchingElbListeners(loadbalancerName, ports).forEach(l -> enableStickinessOnListener(loadbalancerName, l));
    }

    public void disableStickiness(String loadbalancerName, List<Integer> ports) {
        describeMatchingElbListeners(loadbalancerName, ports).forEach(l -> disableStickinessOnListener(loadbalancerName, l));
        awsElbClient.deleteLoadBalancerPolicy(new DeleteLoadBalancerPolicyRequest().withLoadBalancerName(loadbalancerName).withPolicyName(DEPLOY_STICKINESS_POLICY + "-" + loadbalancerName));
    }

    private List<ListenerDescription> describeMatchingElbListeners(String loadbalancerName, List<Integer> ports) {
        DescribeLoadBalancersResult loadbalancer = awsElbClient.describeLoadBalancers(new DescribeLoadBalancersRequest().withLoadBalancerNames(loadbalancerName));
        LoadBalancerDescription description = loadbalancer.getLoadBalancerDescriptions().get(0);
        return description.getListenerDescriptions().stream()
                .filter(d -> ports.contains(d.getListener().getLoadBalancerPort()))
                .filter(d -> d.getListener().getProtocol().startsWith("HTTP"))
                .collect(Collectors.toList());
    }

    private void enableStickinessOnListener(String loadbalancerName, ListenerDescription listenerDescription) {
        log.info("Enable stickiness on loadbalancer " + loadbalancerName + " : " + listenerDescription.getListener().getLoadBalancerPort());
        List<String> policyNames = new ArrayList<>(listenerDescription.getPolicyNames());
        policyNames.add(DEPLOY_STICKINESS_POLICY + "-" + loadbalancerName);
        awsElbClient.setLoadBalancerPoliciesOfListener(new SetLoadBalancerPoliciesOfListenerRequest().withLoadBalancerName(loadbalancerName).withPolicyNames(policyNames).withLoadBalancerPort(listenerDescription.getListener().getLoadBalancerPort()));
    }

    private void disableStickinessOnListener(String loadbalancerName, ListenerDescription listenerDescription) {
        log.info("Disable stickiness on loadbalancer " + loadbalancerName + " : " + listenerDescription.getListener().getLoadBalancerPort());
        List<String> policyNames = new ArrayList<>(listenerDescription.getPolicyNames());
        policyNames.remove(DEPLOY_STICKINESS_POLICY + "-" + loadbalancerName);
        awsElbClient.setLoadBalancerPoliciesOfListener(new SetLoadBalancerPoliciesOfListenerRequest().withLoadBalancerName(loadbalancerName).withPolicyNames(policyNames).withLoadBalancerPort(listenerDescription.getListener().getLoadBalancerPort()));
    }

    public boolean shouldAddExtraInstance(AutoScalingGroup autoScalingGroup) {
        return autoScalingGroup.getInstances().size() < autoScalingGroup.getMaxSize() && !(activeConfiguration.getMaxCapacity() != -1 && autoScalingGroup.getInstances().size() > activeConfiguration.getMaxCapacity());
    }

    private Ec2Instance toEc2Instance(com.amazonaws.services.ec2.model.Instance instance) {
        return new Ec2Instance.Builder().withInstanceId(instance.getInstanceId()).withPrivateIp(instance.getPrivateIpAddress()).withPublicIp(instance.getPublicIpAddress()).build();
    }

    public boolean setDesiredCapacity(AutoScalingGroup autoScalingGroup, Integer capacity) {
        log.info("Setting desired capacity to : " + capacity);

        try {
            awsAsClient.setDesiredCapacity(new SetDesiredCapacityRequest()
                    .withAutoScalingGroupName(autoScalingGroup.getAutoScalingGroupName())
                    .withDesiredCapacity(capacity)
                    .withHonorCooldown(false));
            return true;
        } catch (AmazonClientException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private void updateInstancesStateOnLoadBalancer(String loadBalancerName, List<Ec2Instance> instances) {
        DescribeInstanceHealthResult result = awsElbClient.describeInstanceHealth(new DescribeInstanceHealthRequest(loadBalancerName));
        instances.forEach(i -> result.getInstanceStates().stream().filter(s -> s.getInstanceId().equals(i.getInstanceId())).findFirst().ifPresent(s -> i.updateState(AwsState.map(s.getState()))));
    }

    public void updateInstanceState(Ec2Instance instance, List<String> loadBalancerNames) {
        for (String elb : loadBalancerNames) {
            DescribeInstanceHealthResult result = awsElbClient.describeInstanceHealth(new DescribeInstanceHealthRequest(elb));
            Optional<InstanceState> state = result.getInstanceStates().stream().filter(s -> s.getInstanceId().equals(instance.getInstanceId())).findFirst();
            if (!state.isPresent()) {
                instance.updateState(AwsState.UNKNOWN);
            } else {
                instance.updateState(AwsState.valueOf(state.get().getState().toUpperCase()));
            }
        }
    }

    public boolean checkInstanceInServiceOnAllElb(Instance newInstance, List<String> loadBalancerNames) {
        if (newInstance == null) {
            throw new IllegalStateException("Unable to check null instance");
        }
        for (String elb : loadBalancerNames) {
            DescribeInstanceHealthResult result = awsElbClient.describeInstanceHealth(new DescribeInstanceHealthRequest(elb));
            Optional<InstanceState> state = result.getInstanceStates().stream().filter(s -> s.getInstanceId().equals(newInstance.getInstanceId())).findFirst();
            if (!state.isPresent()) {
                log.info("instance state for instance " + newInstance.getInstanceId() + " on elb " + elb + " is unknown");
                return false;
            }
            log.info("instance state for instance " + newInstance.getInstanceId() + " on elb " + elb + " is " + state.get().getState());
            if (!"InService".equals(state.get().getState())) {
                return false;
            }
        }
        return true;
    }

    public void enableAsGroup(String autoScalingGroupName) {
        awsAsClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(autoScalingGroupName).withDesiredCapacity(1));
    }

    public boolean checkEc2Instance(String instanceId) {
        boolean instanceTerminated = false;
        try {
            DescribeInstancesResult result = awsEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
            List<com.amazonaws.services.ec2.model.Instance> instances = result.getReservations().stream()
                    .flatMap(r -> r.getInstances().stream())
                    .filter(i -> i.getInstanceId().equals(instanceId))
                    .collect(Collectors.toList());
            instanceTerminated = instances.isEmpty() || instances.stream()
                    .map(com.amazonaws.services.ec2.model.Instance::getState)
                    .anyMatch(s -> s.getCode().equals(48));
        } catch (AmazonServiceException e) {
            log.info(e.toString(), e);
            if (e.getStatusCode() == 400) {
                instanceTerminated = true;
            }
        }

        if (instanceTerminated) {
            log.warn("Invalid instance " + instanceId + " in group " + activeConfiguration.getAutoScalingGroupId() + ". Detaching instance.");
            awsAsClient.detachInstances(new DetachInstancesRequest()
                    .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId())
                    .withInstanceIds(instanceId)
                    .withShouldDecrementDesiredCapacity(false));
        }
        return instanceTerminated;
    }

    public void setDeployMetadataTags(final String version, Properties properties) {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().withPropagateAtLaunch(true)
                .withResourceType(AUTO_SCALING_GROUP)
                .withKey(LATEST_REQUEST_TAG).withValue(version)
                .withResourceId(activeConfiguration.getAutoScalingGroupId()));
        tags.add(new Tag().withPropagateAtLaunch(true)
                .withResourceType(AUTO_SCALING_GROUP)
                .withKey(SCOPE_TAG).withValue(Boolean.toString(activeConfiguration.isTestScope()))
                .withResourceId(activeConfiguration.getAutoScalingGroupId()));

        if (!activeConfiguration.getAutoScalingProperties().isEmpty()) {
            tags.add(new Tag().withPropagateAtLaunch(true)
                    .withResourceType(AUTO_SCALING_GROUP)
                    .withKey(PROPERTIES_TAGS).withValue(activeConfiguration.getAutoScalingProperties().stream().map(key -> key + ":" + getProperty(key, properties)).collect(Collectors.joining(";")))
                    .withResourceId(activeConfiguration.getAutoScalingGroupId())
            );
        }
        if (!activeConfiguration.getExclusions().isEmpty()) {
            tags.add(new Tag().withPropagateAtLaunch(true)
                    .withResourceType(AUTO_SCALING_GROUP)
                    .withKey(EXCLUSION_TAG).withValue(activeConfiguration.getExclusions().stream().map(e -> e.getGroupId() + ":" + e.getGroupId()).collect(Collectors.joining(";")))
                    .withResourceId(activeConfiguration.getAutoScalingGroupId()));
        }
        awsAsClient.createOrUpdateTags(new CreateOrUpdateTagsRequest().withTags(tags));
    }

    private String getProperty(String key, Properties properties) {
        return System.getProperty(key, properties.getProperty(key));
    }

    private String matchAutoScalingGroupName(String regex) {
        DescribeAutoScalingGroupsResult result = awsAsClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest());
        List<String> groups = toGroupNameList(result.getAutoScalingGroups());
        while (result.getNextToken() != null && !result.getNextToken().isEmpty()) {
            result = awsAsClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withNextToken(result.getNextToken()));
            groups.addAll(toGroupNameList(result.getAutoScalingGroups()));
        }

        List<String> matchedGroups = groups.stream().filter(name -> name.matches(regex)).collect(Collectors.toList());
        if (matchedGroups == null || matchedGroups.isEmpty() || matchedGroups.size() != 1) {
            int matchSize = matchedGroups == null ? -1 : matchedGroups.size();
            if (matchedGroups != null && matchSize > 0) {
                matchedGroups.forEach(group -> log.error("Matched group : " + group));
            }
            throw new IllegalStateException("Unable to match group regex, matched group size " + matchSize);
        }

        return matchedGroups.stream().findFirst().orElse(regex);
    }

    private List<String> toGroupNameList(List<AutoScalingGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }
        return groups.stream().map(AutoScalingGroup::getAutoScalingGroupName).collect(Collectors.toList());
    }
}
