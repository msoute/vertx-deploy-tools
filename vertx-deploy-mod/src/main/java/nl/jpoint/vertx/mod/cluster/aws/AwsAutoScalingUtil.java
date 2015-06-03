package nl.jpoint.vertx.mod.cluster.aws;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AwsAutoScalingUtil {
    private final static String AWS_AUTOSCALING_SERVICE = "autoscaling";
    private static final String DEFAULT_REGION = "eu-west-1";

    private final String targetHost = AWS_AUTOSCALING_SERVICE + "." + DEFAULT_REGION + ".amazonaws.com";
    private final AwsUtil awsUtil;

    private final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    @Deprecated
    public AwsAutoScalingUtil(final String accessKey, final String secretAccessKey) {
        this(AwsContext.build(accessKey, secretAccessKey, DEFAULT_REGION));
    }

    public AwsAutoScalingUtil(final AwsContext context) {
        awsUtil = context.getAwsUtil();
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public List<String> listInstancesInGroup(final String groupId) throws AwsException {
        return AwsXpathUtil.listInstancesInAutoscalingGroupResponse(describeAutoScalingGroup(groupId));
    }

    public AwsState getInstanceState(final String instanceId, final String groupId) throws AwsException {
        return AwsXpathUtil.getInstanceState(instanceId, describeAutoScalingGroup(groupId));
    }

    public List<String> listLoadBalancers(final String groupId) throws AwsException {
        return AwsXpathUtil.listLoadBalancersInGroup(describeAutoScalingGroup(groupId));
    }

    public boolean enterStandby(final String instanceId, final String groupId, boolean decrementDesiredCapacity) {
        String date = compressedIso8601DateFormat.format(new Date());

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("AutoScalingGroupName", groupId);
        requestParameters.put("InstanceIds.member.1", instanceId);
        requestParameters.put("Version", "2011-01-01");
        requestParameters.put("ShouldDecrementDesiredCapacity", Boolean.toString(decrementDesiredCapacity));
        requestParameters.put("Action", "EnterStandby");

        try {
            HttpGet awsGet = awsUtil.createSignedGet(targetHost, requestParameters, signedHeaders, date, AWS_AUTOSCALING_SERVICE, "eu-west-1", "EnterStandby");
            this.executeRequest(awsGet);
        } catch (AwsException e) {
            return false;
        }
        return true;
    }

    public boolean exitStandby(final String instanceId, final String groupId) {
        String date = compressedIso8601DateFormat.format(new Date());

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("AutoScalingGroupName", groupId);
        requestParameters.put("InstanceIds.member.1", instanceId);
        requestParameters.put("Version", "2011-01-01");
        requestParameters.put("ShouldDecrementDesiredCapacity", "true");
        requestParameters.put("Action", "ExitStandby");

        try {
            HttpGet awsGet = awsUtil.createSignedGet(targetHost, requestParameters, signedHeaders, date, AWS_AUTOSCALING_SERVICE, "eu-west-1", "ExitStandby");
            this.executeRequest(awsGet);
        } catch (AwsException e) {
            return false;
        }

        return true;
    }

    private byte[] describeAutoScalingGroup(final String groupId) throws AwsException {
        String date = compressedIso8601DateFormat.format(new Date());

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("AutoScalingGroupNames.member.1", groupId);
        requestParameters.put("MaxRecords", "20");
        requestParameters.put("Version", "2011-01-01");
        requestParameters.put("Action", "DescribeAutoScalingGroups");

        HttpGet awsGet = awsUtil.createSignedGet(targetHost, requestParameters, signedHeaders, date, AWS_AUTOSCALING_SERVICE, "eu-west-1", "DescribeAutoScalingGroups");

        return this.executeRequest(awsGet);
    }

    private byte[] executeRequest(final HttpGet awsGet) throws AwsException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(awsGet)) {
                return EntityUtils.toByteArray(response.getEntity());
            }
        } catch (IOException e) {
            throw new AwsException(e);
        }
    }

    private Map<String, String> createDefaultSignedHeaders(String date, String targetHost) {
        Map<String, String> signedHeaders = new HashMap<>();
        signedHeaders.put("X-Amz-Date", date);
        signedHeaders.put("Host", targetHost);
        return signedHeaders;
    }
}
