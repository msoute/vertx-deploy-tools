package nl.jpoint.maven.vertx.utils;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AwsEc2UtilTest {
    private static final String AWS_ACCESS_KEY = "";
    private static final String AWS_SECRET_ACCESS_KEY = "";
    private static final String AWS_AS_GROUP = "";
    private static final Log LOG = new DefaultLog(new ConsoleLogger(Logger.LEVEL_DEBUG, "logger"));

    @Test
    @Ignore
    public void testDescribeInstances() throws Exception {
        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AwsEc2Util ec2Util = new AwsEc2Util(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);

        List<String> instanceIds = awsAutoScalingUtil.listInstancesInGroup(AWS_AS_GROUP, LOG);

        List<Ec2Instance> instances = ec2Util.describeInstances(instanceIds, LOG);

        System.out.println(instances.size());

    }
}