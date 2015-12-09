package nl.jpoint.vertx.mod.deploy.aws;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class AwsAutoScalingUtilTest {

    private final static String ACCESS_KEY = "ak";
    private final static String SECRET_ACCESS_KEY = "sak";
    private final static String AWS_REGION = "eu-west-1";
    private final static String AS_GROUP_ID = "gid";
    private final static String INSTANCE_ID = "iid";
    private final static String ELB = "elb";

    private final AwsContext context = AwsContext.build(ACCESS_KEY, SECRET_ACCESS_KEY, AWS_REGION);

    private AwsAutoScalingUtil util = new AwsAutoScalingUtil(context);
    private AwsElbUtil elbUtil = new AwsElbUtil(context, INSTANCE_ID, ELB);

    @Test
    @Ignore
    public void testElbInstanceState() throws AwsException {
        AwsState state = elbUtil.getInstanceState();
        Assert.assertEquals(AwsState.INSERVICE, state);
    }

    @Test
    @Ignore
    public void testListInstancesInGroup() throws AwsException {
        List<String> result = util.listInstancesInGroup("");
        Assert.assertEquals(1, result.size());
    }

    @Test
    @Ignore
    public void testListLoadBalancersInGroup() throws AwsException {
        List<String> result = util.listLoadBalancers(AS_GROUP_ID);
        Assert.assertEquals(1, result.size());
    }

    @Test
    @Ignore
    public void testFetchInstanceState() throws AwsException {
        AwsState result = util.getInstanceState(INSTANCE_ID, AS_GROUP_ID);
        Assert.assertEquals(AwsState.INSERVICE, result);
    }

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