package nl.jpoint.maven.vertx.utils;


import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileInputStream;
import java.io.IOException;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AwsDeployUtilsTest {

    @Mock
    DeployConfiguration deployConfiguration;

    private final Settings settings;

    private AwsAutoScalingDeployUtils deployUtils;

    public AwsDeployUtilsTest() throws IOException, XmlPullParserException {
        settings = new SettingsXpp3Reader().read(new FileInputStream(System.getProperty("user.home")+"/.m2/settings.xml"));
    }

    @Before
    public void init() throws IOException, XmlPullParserException, MojoFailureException {
        deployUtils = new AwsAutoScalingDeployUtils(settings.getServer("deploy-mod-test"), "eu-west-1", deployConfiguration);
        when(deployConfiguration.getAutoScalingGroupId()).thenReturn(settings.getServer("deploy-mod-test").getPassphrase());
    }


    @Test
    @Ignore
    public void testSuspendScheduledActions() throws Exception {
        deployUtils.suspendScheduledActions(null);
        deployUtils.setMinimalCapacity(null, 1);
    }

    @Test
    @Ignore
    public void testResumeScheduledActions() throws Exception {
        deployUtils.setMinimalCapacity(null, 2);
        deployUtils.resumeScheduledActions(null);
    }
}