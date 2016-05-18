package nl.jpoint.maven.vertx.utils;


import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AwsDeployUtilsTest {

    private final Settings settings;
    @Mock
    DeployConfiguration deployConfiguration;
    @Mock
    DefaultLog log;
    private AwsAutoScalingDeployUtils deployUtils;

    public AwsDeployUtilsTest() throws IOException, XmlPullParserException {
        settings = new SettingsXpp3Reader().read(new FileInputStream(System.getProperty("user.home") + "/.m2/settings.xml"));
    }

    @Before
    public void init() throws IOException, XmlPullParserException, MojoFailureException {
        deployUtils = new AwsAutoScalingDeployUtils(settings.getServer("deploy-test"), "eu-west-1", deployConfiguration, null);
        when(deployConfiguration.getAutoScalingGroupId()).thenReturn(settings.getServer("deploy-test").getPassphrase());
    }


    @Test
    @Ignore
    public void testSuspendScheduledActions() throws Exception {
        deployUtils.suspendScheduledActions();
        deployUtils.setMinimalCapacity(1);
    }

    @Test
    @Ignore
    public void testResumeScheduledActions() throws Exception {
        deployUtils.setMinimalCapacity(2);
        deployUtils.resumeScheduledActions();
    }

    @Test
    @Ignore
    public void testInStandByInstancesFirst() throws Exception {
        AutoScalingGroup asgroup = deployUtils.getAutoScalingGroup();
        Assert.assertEquals(2, asgroup.getInstances().size());
        List<Ec2Instance> instances = deployUtils.getInstancesForAutoScalingGroup(log, asgroup);
        Assert.assertEquals(2, instances.size());
    }

    @Test
    @Ignore
    public void testSort() {
        Ec2Instance e1 = new Ec2Instance.Builder().withInstanceId("1").withPrivateIp("0").withPublicIp("0").build();
        e1.updateAsState(AwsState.STANDBY);
        Ec2Instance e2 = new Ec2Instance.Builder().withInstanceId("2").withPrivateIp("0").withPublicIp("0").build();
        e2.updateAsState(AwsState.STANDBY);
        e2.updateState(AwsState.INSERVICE);
        Ec2Instance e3 = new Ec2Instance.Builder().withInstanceId("3").withPrivateIp("0").withPublicIp("0").build();
        e3.updateAsState(AwsState.INSERVICE);
        e3.updateState(AwsState.OUTOFSERVICE);
        Ec2Instance e4 = new Ec2Instance.Builder().withInstanceId("4").withPrivateIp("0").withPublicIp("0").build();
        e4.updateAsState(AwsState.INSERVICE);
        e4.updateState(AwsState.INSERVICE);


        List<Ec2Instance> ec2Instances = Arrays.asList(e2, e4, e1, e3);

        Collections.sort(ec2Instances, (o1, o2) -> {

            int sComp = o1.getAsState().compareTo(o2.getAsState());

            if (sComp != 0) {
                return sComp;
            } else {
                return o1.getElbState().compareTo(o2.getElbState());
            }
        });

        Assert.assertEquals("1", ec2Instances.get(0).getInstanceId());
        Assert.assertEquals("2", ec2Instances.get(1).getInstanceId());
        Assert.assertEquals("3", ec2Instances.get(2).getInstanceId());
        Assert.assertEquals("4", ec2Instances.get(3).getInstanceId());
    }

}