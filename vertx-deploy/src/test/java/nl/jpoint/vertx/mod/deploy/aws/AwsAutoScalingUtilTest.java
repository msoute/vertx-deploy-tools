package nl.jpoint.vertx.mod.deploy.aws;

import org.junit.Ignore;
import org.junit.Test;

public class AwsAutoScalingUtilTest {

    private final static String AWS_REGION = "eu-west-1";
    private final static String AS_GROUP_ID = "gid";
    private final static String INSTANCE_ID = "iid";
    private final static String ELB = "elb";

    private final AwsContext context = AwsContext.build(AWS_REGION);

    private AwsAutoScalingUtil util = new AwsAutoScalingUtil(context);
    private AwsElbUtil elbUtil = new AwsElbUtil(context);


    @Test
    @Ignore
    public void testEnterStandby() throws AwsException {
        util.enterStandby(INSTANCE_ID, AS_GROUP_ID, false);
    }

    @Test
    @Ignore
    public void testExitStandby() throws AwsException {
        util.exitStandby(INSTANCE_ID, AS_GROUP_ID);
    }
}