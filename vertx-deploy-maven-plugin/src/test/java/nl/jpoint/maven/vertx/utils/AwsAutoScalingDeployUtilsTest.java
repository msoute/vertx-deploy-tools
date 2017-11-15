package nl.jpoint.maven.vertx.utils;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AwsAutoScalingDeployUtilsTest {

    @Mock
    private DeployConfiguration deployConfiguration;

    @Mock
    private AutoScalingGroup autoScalingGroup;

    @Mock
    private List<Instance> instances;

    @Test
    public void testShouldAddExtraInstance() throws Exception {
        when(deployConfiguration.getMaxCapacity()).thenReturn(4);
        when(autoScalingGroup.getMaxSize()).thenReturn(4);
        when(instances.size()).thenReturn(1);
        when(autoScalingGroup.getInstances()).thenReturn(instances);
        AwsAutoScalingDeployUtils utils = new AwsAutoScalingDeployUtils("eu-west-1", deployConfiguration, null);
        Assert.assertTrue(utils.shouldAddExtraInstance(autoScalingGroup));
    }

    @Test
    public void testShouldNotAddExtraInstance() throws Exception {
        when(deployConfiguration.getMaxCapacity()).thenReturn(4);
        when(autoScalingGroup.getMaxSize()).thenReturn(4);
        when(instances.size()).thenReturn(4);
        when(autoScalingGroup.getInstances()).thenReturn(instances);
        AwsAutoScalingDeployUtils utils = new AwsAutoScalingDeployUtils("eu-west-1", deployConfiguration, null);
        Assert.assertFalse(utils.shouldAddExtraInstance(autoScalingGroup));
    }

    @Test
    public void testShouldNotAddExtraInstance_NoConfiguredMax() throws Exception {
        when(deployConfiguration.getMaxCapacity()).thenReturn(-1);
        when(autoScalingGroup.getMaxSize()).thenReturn(4);
        when(instances.size()).thenReturn(4);
        when(autoScalingGroup.getInstances()).thenReturn(instances);
        AwsAutoScalingDeployUtils utils = new AwsAutoScalingDeployUtils("eu-west-1", deployConfiguration, null);
        Assert.assertFalse(utils.shouldAddExtraInstance(autoScalingGroup));
    }
}