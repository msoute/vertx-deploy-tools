package nl.jpoint.maven.vertx.utils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.util.List;

public class AwsDeployUtils {

    private final AwsAutoScalingUtil awsAutoScalingUtil;
    private final AwsEc2Util awsEc2Util;
    private final AwsOpsWorksUtil opsWorksUtil;
    private final AwsElbUtil awsElbUtil;

    AmazonAutoScalingClient awsAsClient;


    public AwsDeployUtils(String serverId, Settings settings) throws MojoFailureException {
        if (settings.getServer(serverId) == null) {
            throw new MojoFailureException("No server config for id : " + serverId);
        }
        Server server = settings.getServer(serverId);

        awsAsClient = new AmazonAutoScalingClient(new BasicAWSCredentials(server.getUsername(), server.getPassword()));
        awsAsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));

        awsAutoScalingUtil = new AwsAutoScalingUtil(server.getUsername(), server.getPassword());
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
                .withScalingProcesses("AZRebalance", "ScheduledActions", "Terminate", "ReplaceUnhealthy")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Suspended autoscaling processes.");
    }

    public void setMinimalCapacity(Log log, int cap, DeployConfiguration activeConfiguration) {
        log.info("Set minimal capacity for group to " + cap);
        awsAsClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()).withMinSize(cap));
    }

    public void resumeScheduledActions(Log log, DeployConfiguration activeConfiguration) {
        awsAsClient.resumeProcesses(new ResumeProcessesRequest()
                .withScalingProcesses("AZRebalance","ScheduledActions", "Terminate", "ReplaceUnhealthy")
                .withAutoScalingGroupName(activeConfiguration.getAutoScalingGroupId()));
        log.info("Resumed autoscaling processes.");
    }


    public List<Ec2Instance> getInstancesForAutoScalingGroup(Log log, DeployConfiguration activeConfiguration) throws MojoFailureException, MojoExecutionException {
        log.info("retrieving list of instanceId's for auto scaling group with id : " + activeConfiguration.getAutoScalingGroupId());
        activeConfiguration.getHosts().clear();

        try {
            log.debug("describing Autoscaling group");
            AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(activeConfiguration.getAutoScalingGroupId(), activeConfiguration.isIgnoreInStandby(), log);
            if (!autoScalingGroup.deployable() && !activeConfiguration.isIgnoreDeployState()) {
                throw new MojoExecutionException("Autoscaling group is not in a deployable state.");
            }
            log.debug("describing instances in Autoscaling group");
            List<Ec2Instance> instances = awsEc2Util.describeInstances(autoScalingGroup.getInstances(), activeConfiguration.getTag(), log);
            log.debug("describing elb status");
            return awsElbUtil.describeInstanceElbStatus(instances, autoScalingGroup.getElbs());

        } catch (AwsException e) {
            throw new MojoFailureException(e.getMessage());
        }
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

}
