package nl.jpoint.maven.vertx.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;

public class AwsXpathUtilTest {

    byte[] data;

    @Before
    public void init() throws IOException {
        data = Files.readAllBytes(Paths.get(AwsXpathUtil.class.getClassLoader().getResource("asresponse.xml").getPath()));
    }

    @Test
    public void testListPrivateDNSInDescribeInstancesResponse() throws Exception {

    }

    @Test
    public void testListInstancesInAutoscalingGroupResponse() throws Exception {

    }

    @Test
    public void testListELBsInAutoscalingGroupResponse() throws Exception {

    }

    @Test
    public void testListMinimalInstancesInAutoscalingGroupResponse() throws Exception {
        Assert.assertThat(AwsXpathUtil.listMinimalInstancesInAutoscalingGroupResponse(data), is(2));
    }

    @Test
    public void testListMaximumInstancesInAutoscalingGroupResponse() throws Exception {
        Assert.assertThat(AwsXpathUtil.listMaximumInstancesInAutoscalingGroupResponse(data), is(10));
    }

    @Test
    public void testUpdateInstanceState() throws Exception {

    }

    @Test
    public void testDescribeInstances() throws Exception {

    }
}