package nl.jpoint.maven.vertx.utils;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AwsAutoScalingUtil {
    private final static String AWS_AUTOSCALING_SERVICE = "autoscaling";
    private final String targetHost = AWS_AUTOSCALING_SERVICE + "." + "eu-west-1" + ".amazonaws.com";

    private final AwsUtil awsUtil;

    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    public AwsAutoScalingUtil(String accessKey, String secretAccessKey) {
        this.awsUtil = new AwsUtil(accessKey, secretAccessKey);
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public AutoScalingGroup describeAutoScalingGroup(String groupId, Log log) throws AwsException {
        String date = compressedIso8601DateFormat.format(new Date());

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);
        Map<String, String> requestParamerters = new HashMap<>();
        requestParamerters.put("AutoScalingGroupNames.member.1", groupId);
        requestParamerters.put("MaxRecords", "20");
        requestParamerters.put("Version", "2011-01-01");
        requestParamerters.put("Action", "DescribeAutoScalingGroups");

        HttpGet awsGet = awsUtil.createSignedGet(targetHost, requestParamerters, signedHeaders, date, AWS_AUTOSCALING_SERVICE, "eu-west-1", "DescribeAutoScalingGroups");

        byte[] result = this.executeRequest(awsGet);

        return new AutoScalingGroup.Builder()
                .withInstances(AwsXpathUtil.listInstancesInAutoscalingGroupResponse(result))
                .withElbs(AwsXpathUtil.listELBsInAutoscalingGroupResponse(result))
                .build();

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
