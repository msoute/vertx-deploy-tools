package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

import java.util.List;

public class AwsEc2UtilTest {
    private static final String AWS_ACCESS_KEY = "";
    private static final String AWS_SECRET_ACCESS_KEY = "";
    private static final String AWS_AS_GROUP = "";

    private static final Log LOG = new DefaultLog(new ConsoleLogger(Logger.LEVEL_DEBUG, "logger"));

    @Test

    public void testDescribeInstances() throws Exception {

        DeployConfiguration config = new DeployConfiguration();
        //config.setAutoScalingGroupId(AWS_AS_GROUP);

        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AwsEc2Util ec2Util = new AwsEc2Util(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AwsElbUtil elbUtil = new AwsElbUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);

        AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(config.getAutoScalingGroupId(), LOG);
        List<Ec2Instance> instances = ec2Util.describeInstances(autoScalingGroup.getInstances(), config.getTag(), LOG);
        elbUtil.describeInstanceElbStatus(instances, autoScalingGroup.getElbs());

     /*   AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(AWS_AS_GROUP, LOG);

        List<Ec2Instance> instances = ec2Util.describeInstances(autoScalingGroup.getInstances(),null, LOG);
*/
        //elbUtil.describeInstanceElbStatus(instances, autoScalingGroup.getElbs());

    }

    @Test
    public void testLabelCheck() throws AwsException {
        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(AWS_AS_GROUP, LOG);

    }

}