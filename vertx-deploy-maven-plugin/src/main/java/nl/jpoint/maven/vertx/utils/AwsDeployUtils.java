package nl.jpoint.maven.vertx.utils;

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

    public AwsDeployUtils(String serverId, Settings settings) throws MojoFailureException {
        if (settings.getServer(serverId) == null) {
            throw new MojoFailureException("No server config for id : " + serverId);
        }
        Server server = settings.getServer(serverId);

        awsAutoScalingUtil = new AwsAutoScalingUtil(server.getUsername(), server.getPassword());
        awsEc2Util = new AwsEc2Util(server.getUsername(), server.getPassword());
        opsWorksUtil = new AwsOpsWorksUtil(server.getUsername(), server.getPassword());
        awsElbUtil = new AwsElbUtil(server.getUsername(), server.getPassword());
    }

    public List<Ec2Instance> getInstancesForAutoScalingGroup(Log log, DeployConfiguration activeConfiguration) throws MojoFailureException, MojoExecutionException {
        log.info("retrieving list of instanceId's for auto scaling group with id : " + activeConfiguration.getAutoScalingGroupId());
        activeConfiguration.getHosts().clear();

        try {
            log.debug("describing Autoscaling group");
            AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(activeConfiguration.getAutoScalingGroupId(), activeConfiguration.isIgnoreInStandby(), log);
            if (!autoScalingGroup.deployable()) {
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
