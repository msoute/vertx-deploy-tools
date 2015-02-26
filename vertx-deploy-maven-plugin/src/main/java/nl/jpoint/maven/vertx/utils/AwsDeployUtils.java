package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.util.List;

public class AwsDeployUtils {

    public static List<Ec2Instance> getInstancesForAutoScalingGroup(Log log, DeployConfiguration activeConfiguration, Settings settings) throws MojoFailureException {
        log.info("retrieving list of instanceId's for auto scaling group with id : " + activeConfiguration.getAutoScalingGroupId());
        activeConfiguration.getHosts().clear();
        if (settings.getServer(activeConfiguration.getAutoScalingGroupId()) == null) {
            throw new MojoFailureException("No server config for auto scaling group id : " + activeConfiguration.getAutoScalingGroupId());
        }
        Server server = settings.getServer(activeConfiguration.getAutoScalingGroupId());
        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(server.getUsername(), server.getPassword());
        AwsEc2Util awsEc2Util = new AwsEc2Util(server.getUsername(), server.getPassword());

        try {
            List<String> instanceIds = awsAutoScalingUtil.listInstancesInGroup(activeConfiguration.getAutoScalingGroupId(), log);
            return awsEc2Util.describeInstances(instanceIds, log);
        } catch (AwsException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    public static void getHostsOpsWorks(Log log, DeployConfiguration activeConfiguration, Settings settings) throws MojoFailureException {
        log.info("retrieving list of hosts for stack with id : " + activeConfiguration.getOpsWorksStackId());
        activeConfiguration.getHosts().clear();
        if (settings.getServer(activeConfiguration.getOpsWorksStackId()) == null) {
            throw new MojoFailureException("No server config for stack id : " + activeConfiguration.getOpsWorksStackId());
        }
        Server server = settings.getServer(activeConfiguration.getOpsWorksStackId());
        AwsOpsWorksUtil opsWorksUtil = new AwsOpsWorksUtil(server.getUsername(), server.getPassword());
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
