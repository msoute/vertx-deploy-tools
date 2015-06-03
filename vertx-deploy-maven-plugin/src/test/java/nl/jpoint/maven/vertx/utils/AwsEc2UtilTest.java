package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class AwsEc2UtilTest {
    private String AWS_ACCESS_KEY = "";
    private String AWS_SECRET_ACCESS_KEY = "";
    private String AWS_AS_GROUP = "";
    private static final String SERVER_ID = "deploy-mod-test";
    private static final Log LOG = new DefaultLog(new ConsoleLogger(Logger.LEVEL_DEBUG, "logger"));
    private Settings settings;



    @Before
    public void init() throws IOException, XmlPullParserException {
        Reader reader = new FileReader(new File(System.getProperty("user.home")+"/.m2/settings.xml"));
        settings = new SettingsXpp3Reader().read(reader);
        Server server = settings.getServer(SERVER_ID);
        AWS_ACCESS_KEY = server.getUsername();
        AWS_SECRET_ACCESS_KEY = server.getPassword();
        AWS_AS_GROUP = server.getPassphrase();
    }

    @Test
    public void describeInstancesInAutoScalingGroup() throws AwsException {
        DeployConfiguration config = new DeployConfiguration();
        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AwsEc2Util ec2Util = new AwsEc2Util(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(AWS_AS_GROUP, false, LOG);
        List<Ec2Instance> instances = ec2Util.describeInstances(autoScalingGroup.getInstances(), config.getTag(), LOG);
        Assert.assertEquals(2, instances.size());
    }

    @Test
    @Ignore
    public void testDescribeInstances() throws Exception {

        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AwsEc2Util ec2Util = new AwsEc2Util(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AwsElbUtil elbUtil = new AwsElbUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);

        AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(AWS_AS_GROUP, false, LOG);
        List<Ec2Instance> instances = ec2Util.describeInstances(autoScalingGroup.getInstances(), null, LOG);
        elbUtil.describeInstanceElbStatus(instances, autoScalingGroup.getElbs());

     /*   AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(AWS_AS_GROUP, LOG);

        List<Ec2Instance> instances = ec2Util.describeInstances(autoScalingGroup.getInstances(),null, LOG);
*/
        //elbUtil.describeInstanceElbStatus(instances, autoScalingGroup.getElbs());

    }

    @Test
    @Ignore
    public void testLabelCheck() throws AwsException {
        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(AWS_ACCESS_KEY, AWS_SECRET_ACCESS_KEY);
        AutoScalingGroup autoScalingGroup = awsAutoScalingUtil.describeAutoScalingGroup(AWS_AS_GROUP,false, LOG);

    }

}