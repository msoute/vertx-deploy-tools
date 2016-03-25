package nl.jpoint.maven.vertx.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AwsAutoScalingDeployUtils {

    private static final String LATEST_VERSION_TAG = "deploy:latest:version";
    private static final String SCOPE_TAG = "deploy:scope:tst";
    private static final String EXCLUSION_TAG = "deploy:exclusions";
    private static final String PROPERTIES_TAGS = "deploy:properties";

    private final AmazonAutoScalingClient awsAsClient;
    private final AmazonElasticLoadBalancingClient awsElbClient;
    private final AmazonEC2Client awsEc2Client;
    private final DeployConfiguration activeConfiguration;


    public AwsAutoScalingDeployUtils(Server server, String region, DeployConfiguration activeConfiguration) throws MojoFailureException {
        this.activeConfiguration = activeConfiguration;
        if (server == null) {
            throw new MojoFailureException("No server config provided");
        }
        BasicAWSCredentials credentials = new BasicAWSCredentials(server.getUsername(), server.getPassword());
        Region awsRegion = Region.getRegion(Regions.fromName(region));
        awsAsClient = new AmazonAutoScalingClient(credentials);
        awsAsClient.setRegion(awsRegion);

        awsElbClient = new AmazonElasticLoadBalancingClient(credentials);
        awsElbClient.setRegion(awsRegion);

        awsEc2Client = new AmazonEC2Client(credentials);
        awsEc2Client.setRegion(awsRegion);

    }

    public AutoScalingGroup getAutoScalingGroup() {

        DescribeAutoScalingGroupsResult result = awsAsClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(activeConfiguration.getAutoScalingGroupId()));
        return !result.getAutoScalingGroups().isEmpty() ? result.getAutoScalingGroups().get(0) : null;
    }

    public void suspendScheduledActions(Log log) {
        awsAsClient.suspendProcesses(new SuspendProcessesRequest()
                .withScalingProcesses("ScheduledActions", "Terminate", "ReplaceUnhealthy", "AZRebalance")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Should a build fail the processes can be resumed using the AWS CLI.");
        log.info("aws autoscaling resume-processes --auto-scaling-group-name " + activeConfiguration.getAutoScalingGroupId() + " --scaling-processes AZRebalance ReplaceUnhealthy Terminate ScheduledActions");
        log.info("Suspended auto scaling processes.");
    }

    public void setMinimalCapacity(Log log, int cap) {
        log.info("Set minimal capacity for group to " + cap);
        awsAsClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()).withMinSize(cap));
    }

    public void resumeScheduledActions(Log log) {
        awsAsClient.resumeProcesses(new ResumeProcessesRequest()
                .withScalingProcesses("ScheduledActions", "Terminate", "ReplaceUnhealthy", "AZRebalance")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Resumed auto scaling processes.");
    }

    public List<Ec2Instance> getInstancesForAutoScalingGroup(Log log, AutoScalingGroup autoScalingGroup) throws MojoFailureException, MojoExecutionException {
        log.info("retrieving list of instanceId's for auto scaling group with id : " + activeConfiguration.getAutoScalingGroupId());

        activeConfiguration.getHosts().clear();

        log.debug("describing instances in auto scaling group");

        if (autoScalingGroup.getInstances().isEmpty()) {
            throw new MojoFailureException("No instances in AS group.");
        }

        Map<String, Instance> instanceMap = autoScalingGroup.getInstances().stream().collect(Collectors.toMap(Instance::getInstanceId, Function.identity()));

        try {
            DescribeInstancesResult instancesResult = awsEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(autoScalingGroup.getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList())));
            List<Ec2Instance> ec2Instances = instancesResult.getReservations().stream().flatMap(r -> r.getInstances().stream()).map(this::toEc2Instance).collect(Collectors.toList());
            log.debug("describing elb status");
            autoScalingGroup.getLoadBalancerNames().forEach(elb -> this.updateInstancesStateOnLoadBalancer(elb, ec2Instances));
            ec2Instances.stream().forEach(i -> i.updateAsState(AwsState.map(instanceMap.get(i.getInstanceId()).getLifecycleState())));
            Collections.sort(ec2Instances, (o1, o2) -> {

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
            throw new MojoFailureException(e.getMessage());
        }

    }

    public boolean shouldAddExtraInstance(AutoScalingGroup autoScalingGroup) {
        return autoScalingGroup.getInstances().size() < autoScalingGroup.getMaxSize() && !(activeConfiguration.getMaxCapacity() != -1 && autoScalingGroup.getInstances().size() > activeConfiguration.getMaxCapacity());
    }

    private Ec2Instance toEc2Instance(com.amazonaws.services.ec2.model.Instance instance) {
        return new Ec2Instance.Builder().withInstanceId(instance.getInstanceId()).withPrivateIp(instance.getPrivateIpAddress()).withPublicIp(instance.getPublicIpAddress()).build();
    }

    public boolean setDesiredCapacity(Log log, AutoScalingGroup autoScalingGroup, Integer capacity) {
        log.info("Setting desired capacity to : " + capacity);

        try {
            awsAsClient.setDesiredCapacity(new SetDesiredCapacityRequest()
                    .withAutoScalingGroupName(autoScalingGroup.getAutoScalingGroupName())
                    .withDesiredCapacity(capacity)
                    .withHonorCooldown(false));
            return true;
        } catch (AmazonClientException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    private void updateInstancesStateOnLoadBalancer(String loadBalancerName, List<Ec2Instance> instances) {
        DescribeInstanceHealthResult result = awsElbClient.describeInstanceHealth(new DescribeInstanceHealthRequest(loadBalancerName));
        instances.stream().forEach(i -> result.getInstanceStates().stream().filter(s -> s.getInstanceId().equals(i.getInstanceId())).findFirst().ifPresent(s -> i.updateState(AwsState.map(s.getState()))));
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

    public boolean checkInstanceInServiceOnAllElb(Instance newInstance, List<String> loadBalancerNames, Log log) {
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

    public boolean checkEc2Instance(String instanceId, Log log) {
        boolean instanceTerminated = false;
        try {
            DescribeInstancesResult result = awsEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
            List<com.amazonaws.services.ec2.model.Instance> instances = result.getReservations().stream()
                    .flatMap(r -> r.getInstances().stream())
                    .filter(i -> i.getInstanceId().equals(instanceId))
                    .collect(Collectors.toList());
            instanceTerminated = instances.isEmpty() || instances.stream()
                    .map(com.amazonaws.services.ec2.model.Instance::getState)
                    .filter(s -> s.getCode().equals(48))
                    .findFirst().isPresent();
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
        List<Tag> tags = Arrays.asList(
                new Tag().withPropagateAtLaunch(true)
                        .withResourceType("auto-scaling-group")
                        .withKey(LATEST_VERSION_TAG).withValue(version)
                        .withResourceId(activeConfiguration.getAutoScalingGroupId()),
                new Tag().withPropagateAtLaunch(true)
                        .withResourceType("auto-scaling-group")
                        .withKey(SCOPE_TAG).withValue(Boolean.toString(activeConfiguration.isTestScope()))
                        .withResourceId(activeConfiguration.getAutoScalingGroupId())
        );

        if (!activeConfiguration.getAutoScalingProperies().isEmpty()) {
            tags.add(new Tag().withPropagateAtLaunch(true)
                    .withResourceType("auto-scaling-group")
                    .withKey(PROPERTIES_TAGS).withValue(activeConfiguration.getAutoScalingProperies().stream().map(property -> property + ":" + properties.getProperty(property)).collect(Collectors.joining(";")))
                    .withResourceId(activeConfiguration.getAutoScalingGroupId())
            );
        }
        if (!activeConfiguration.getExclusions().isEmpty()) {
            tags.add(new Tag().withPropagateAtLaunch(true)
                    .withResourceType("auto-scaling-group")
                    .withKey(EXCLUSION_TAG).withValue(activeConfiguration.getExclusions().stream().map(e -> e.getGroupId() + ":" + e.getGroupId()).collect(Collectors.joining(";")))
                    .withResourceId(activeConfiguration.getAutoScalingGroupId()));
        }
        awsAsClient.createOrUpdateTags(new CreateOrUpdateTagsRequest().withTags(tags));
    }
}
