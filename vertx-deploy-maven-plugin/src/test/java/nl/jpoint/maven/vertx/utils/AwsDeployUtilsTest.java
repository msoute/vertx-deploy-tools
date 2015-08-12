package nl.jpoint.maven.vertx.utils;

import jdk.nashorn.internal.ir.annotations.Ignore;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileInputStream;
import java.io.IOException;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AwsDeployUtilsTest {

    @Mock
    DeployConfiguration deployConfiguration;

    private final Settings settings;

    private AwsDeployUtils deployUtils;

    public AwsDeployUtilsTest() throws IOException, XmlPullParserException {
        settings = new SettingsXpp3Reader().read(new FileInputStream(System.getProperty("user.home")+"/.m2/settings.xml"));
    }

    @Before
    public void init() throws IOException, XmlPullParserException, MojoFailureException {
        deployUtils = new AwsDeployUtils("deploy-mod-test", settings);
        when(deployConfiguration.getAutoScalingGroupId()).thenReturn(settings.getServer("deploy-mod-test").getPassphrase());
    }


    @Test
    @Ignore
    public void testSuspendScheduledActions() throws Exception {
        deployUtils.suspendScheduledActions(null, deployConfiguration);
        deployUtils.setMinimalCapacity(1, deployConfiguration);
    }

    @Test
    @Ignore
    public void testResumeScheduledActions() throws Exception {
        deployUtils.setMinimalCapacity(2, deployConfiguration);
        deployUtils.resumeScheduledActions(null, deployConfiguration);
    }
}