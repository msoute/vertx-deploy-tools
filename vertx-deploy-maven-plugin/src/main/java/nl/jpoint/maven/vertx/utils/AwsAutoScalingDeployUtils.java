package nl.jpoint.maven.vertx.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
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
import org.apache.maven.settings.Settings;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AwsAutoScalingDeployUtils {

    private final AmazonAutoScalingClient awsAsClient;
    private final AmazonElasticLoadBalancingClient awsElbClient;
    private final AmazonEC2Client awsEc2Client;

    public AwsAutoScalingDeployUtils(String serverId, Settings settings, String region) throws MojoFailureException {
        if (settings.getServer(serverId) == null) {
            throw new MojoFailureException("No server config for id : " + serverId);
        }
        Server server = settings.getServer(serverId);

        BasicAWSCredentials credentials = new BasicAWSCredentials(server.getUsername(), server.getPassword());
        Region awsRegion = Region.getRegion(Regions.fromName(region));
        awsAsClient = new AmazonAutoScalingClient(credentials);
        awsAsClient.setRegion(awsRegion);

        awsElbClient = new AmazonElasticLoadBalancingClient(credentials);
        awsElbClient.setRegion(awsRegion);

        awsEc2Client = new AmazonEC2Client(credentials);
        awsEc2Client.setRegion(awsRegion);

    }

    public com.amazonaws.services.autoscaling.model.AutoScalingGroup getAutoscalingGroup(DeployConfiguration activeConfiguration) {
        DescribeAutoScalingGroupsResult result = awsAsClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(activeConfiguration.getAutoScalingGroupId()));
        return !result.getAutoScalingGroups().isEmpty() ? result.getAutoScalingGroups().get(0) : null;
    }

    public void suspendScheduledActions(Log log, DeployConfiguration activeConfiguration) {
        awsAsClient.suspendProcesses(new SuspendProcessesRequest()
                .withScalingProcesses("ScheduledActions", "Terminate", "ReplaceUnhealthy")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Suspended autoscaling processes.");
    }

    public void setMinimalCapacity(Log log, int cap, DeployConfiguration activeConfiguration) {
        log.info("Set minimal capacity for group to " + cap);
        awsAsClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()).withMinSize(cap));
    }

    public void resumeScheduledActions(Log log, DeployConfiguration activeConfiguration) {
        awsAsClient.resumeProcesses(new ResumeProcessesRequest()
                .withScalingProcesses("ScheduledActions", "Terminate", "ReplaceUnhealthy")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Resumed autoscaling processes.");
    }

    public List<Ec2Instance> getInstancesForAutoScalingGroup(Log log, AutoScalingGroup autoScalingGroup, DeployConfiguration activeConfiguration) throws MojoFailureException, MojoExecutionException {
        log.info("retrieving list of instanceId's for auto scaling group with id : " + activeConfiguration.getAutoScalingGroupId());
        activeConfiguration.getHosts().clear();

        log.debug("describing instances in Autoscaling group");

        try {
            if (!isDeployable(autoScalingGroup, activeConfiguration.isIgnoreInStandby()) || activeConfiguration.isIgnoreDeployState()) {
                throw new MojoExecutionException("Autoscaling group is not in a deployable state.");
            }
            DescribeInstancesResult instancesResult = awsEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(autoScalingGroup.getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList())));
            List<Ec2Instance> ec2Instances = instancesResult.getReservations().stream().flatMap(r -> r.getInstances().stream()).map(this::toEc2Instance).collect(Collectors.toList());
            log.debug("describing elb status");
            autoScalingGroup.getLoadBalancerNames().forEach(elb -> this.updateInstancesStateOnLoadBalancer(elb, ec2Instances));
            return ec2Instances;

        } catch (AmazonClientException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private Ec2Instance toEc2Instance(com.amazonaws.services.ec2.model.Instance instance) {
        return new Ec2Instance.Builder().withInstanceId(instance.getInstanceId()).withPrivateIp(instance.getPrivateIpAddress()).withPublicIp(instance.getPublicIpAddress()).build();
    }

    private boolean isDeployable(AutoScalingGroup autoScalingGroup, boolean ignoreInStandby) {
        long inServiceInstances = autoScalingGroup.getInstances().stream()
                .filter(i -> i.getLifecycleState().equals(LifecycleState.InService.toString()))
                .count();

        long inStandbyInstances = autoScalingGroup.getInstances().stream()
                .filter(i -> i.getLifecycleState().equals(LifecycleState.Standby.toString()))
                .count();

        long healthyInstances = ignoreInStandby ? inServiceInstances : inServiceInstances + inStandbyInstances;
        return autoScalingGroup.getMinSize() < autoScalingGroup.getDesiredCapacity() && healthyInstances > 1;
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

    public void updateInstancesStateOnLoadBalancer(String loadBalancerName, List<Ec2Instance> instances) {
        DescribeInstanceHealthResult result = awsElbClient.describeInstanceHealth(new DescribeInstanceHealthRequest(loadBalancerName));
        instances.stream().forEach(i -> result.getInstanceStates().stream().filter(s -> s.getInstanceId().equals(i.getInstanceId())).findFirst().ifPresent(s -> i.updateState(AwsState.map(s.getState()))));
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

}
