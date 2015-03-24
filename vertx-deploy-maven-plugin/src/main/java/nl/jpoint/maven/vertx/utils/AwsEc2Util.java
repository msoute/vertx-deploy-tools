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

public class AwsEc2Util {
    private final static String AWS_EC2_SERVICE = "ec2";
    private final String targetHost = AWS_EC2_SERVICE + "." + "eu-west-1" + ".amazonaws.com";

    private final AwsUtil awsUtil;
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    public AwsEc2Util(String accessKey, String secretAccessKey) {
        this.awsUtil = new AwsUtil(accessKey, secretAccessKey);
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public List<String> describeInstance(List<String> instanceIds, Log log) throws AwsException {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return new ArrayList<>();
        }
        String date = compressedIso8601DateFormat.format(new Date());

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);
        Map<String, String> requestParamerters = new HashMap<>();
        for (int i = 0; i < instanceIds.size(); i++) {
            requestParamerters.put("InstanceId."+i, instanceIds.get(i));
        }
        requestParamerters.put("Version", "2014-10-01");
        requestParamerters.put("Action", "DescribeInstances");

        HttpGet awsGet = awsUtil.createSignedGet(targetHost, requestParamerters, signedHeaders, date, AWS_EC2_SERVICE, "eu-west-1", "DescribeInstances");

        byte[] result = this.executeRequest(awsGet);
        return AwsXpathUtil.listPrivateDNSInDescribeInstancesResponse(result);
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
