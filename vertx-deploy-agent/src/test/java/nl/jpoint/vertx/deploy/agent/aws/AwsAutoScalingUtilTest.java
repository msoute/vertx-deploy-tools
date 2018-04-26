package nl.jpoint.vertx.deploy.agent.aws;

import nl.jpoint.vertx.deploy.agent.DeployConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AwsAutoScalingUtilTest {

    @Mock
    DeployConfig config;
    private final static String AWS_REGION = "eu-west-1";
    private final static String AS_GROUP_ID = "gid";
    private final static String INSTANCE_ID = "iid";
    private final static String ELB = "elb";


    private AwsAutoScalingUtil util = new AwsAutoScalingUtil(config);
    private AwsElbUtil elbUtil = new AwsElbUtil(config);


    @Test
    @Ignore
    public void testEnterStandby() throws AwsException {
        util.enterStandby(AS_GROUP_ID, false);
    }

    @Test
    @Ignore
    public void testExitStandby() throws AwsException {
        util.exitStandby(AS_GROUP_ID);
    }
}