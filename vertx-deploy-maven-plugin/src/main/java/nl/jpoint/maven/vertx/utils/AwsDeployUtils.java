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

public class AwsDeployUtils {

    private final AwsEc2Util awsEc2Util;
    private final AwsOpsWorksUtil opsWorksUtil;
    private final AwsElbUtil awsElbUtil;

    AmazonAutoScalingClient awsAsClient;
    AmazonElasticLoadBalancingClient awsElbClient;


    public AwsDeployUtils(String serverId, Settings settings) throws MojoFailureException {
        if (settings.getServer(serverId) == null) {
            throw new MojoFailureException("No server config for id : " + serverId);
        }
        Server server = settings.getServer(serverId);

        BasicAWSCredentials credentials = new BasicAWSCredentials(server.getUsername(), server.getPassword());
        awsAsClient = new AmazonAutoScalingClient(credentials);
        awsAsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));

        awsElbClient = new AmazonElasticLoadBalancingClient(credentials);
        awsElbClient.setRegion(Region.getRegion(Regions.EU_WEST_1));


        awsEc2Util = new AwsEc2Util(server.getUsername(), server.getPassword());
        opsWorksUtil = new AwsOpsWorksUtil(server.getUsername(), server.getPassword());
        awsElbUtil = new AwsElbUtil(server.getUsername(), server.getPassword());
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
            List<Ec2Instance> instances = awsEc2Util.describeInstances(autoScalingGroup.getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList()), activeConfiguration.getTag(), log);
            log.debug("describing elb status");
            return awsElbUtil.describeInstanceElbStatus(instances, autoScalingGroup.getLoadBalancerNames());

        } catch (AwsException | AmazonClientException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private boolean isDeployable(AutoScalingGroup autoScalingGroup, boolean ignoreInStandby) {
        long inServiceInstances = autoScalingGroup.getInstances().stream()
                .filter(i -> i.getLifecycleState().equals(LifecycleState.InService.toString()))
                .count();

        long inStandbyInstances = autoScalingGroup.getInstances().stream()
                .filter(i -> i.getLifecycleState().equals(LifecycleState.Standby.toString()))
                .count();

        long healthyInstances = ignoreInStandby ? inServiceInstances : inServiceInstances + inStandbyInstances;
        return autoScalingGroup.getMinSize() < autoScalingGroup.getDesiredCapacity()  && healthyInstances > 1 ;
    }

    public void getHostsOpsWorks(Log log, DeployConfiguration activeConfiguration) throws MojoFailureException {
        log.info("retrieving list of hosts for stack with id : " + activeConfiguration.getOpsWorksStackId());
        activeConfiguration.getHosts().clear();

        List<String> hosts;
        try {
            hosts = opsWorksUtil.ListStackInstances(activeConfiguration.getOpsWorksStackId(), activeConfiguration.getOpsWorksLayerId(), activeConfiguration.getAwsPrivateIp(), log);
            for (String opsHost : hosts) {
                log.info("Adding host from opsworks response : " + opsHost);
                activeConfiguration.getHosts().add("http://" + opsHost + ":6789");
            }
        } catch (AwsException e) {
            throw new MojoFailureException(e.getMessage());
        }
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

    public boolean getInstanceStatus(Instance newInstance, List<String> loadBalancerNames, Log log) {
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
