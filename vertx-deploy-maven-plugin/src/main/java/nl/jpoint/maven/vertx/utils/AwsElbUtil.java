package nl.jpoint.maven.vertx.utils;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AwsElbUtil {

    private static final String EQUALSSIGN = "=";
    private static final String AWS_ELB_SERVICE = "elasticloadbalancing";
    private static final String AWS_ACTION = "Action";

    private static final String SERVICE_VERSION = "2012-06-01";
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private final String region;
    private final AwsUtil awsUtil;


    public AwsElbUtil(String accessKey, String secretAccessKey) {
        this(new AwsUtil(accessKey, secretAccessKey));
    }

    public AwsElbUtil(AwsUtil awsUtil) {
        this.region = "eu-west-1";
        this.awsUtil = awsUtil;
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private byte[] executeGetInstanceState(final String loadbalancer) throws AwsException {
        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, AWS_ACTION + EQUALSSIGN + "DescribeInstanceHealth" + "&" + "LoadBalancerName" + EQUALSSIGN + loadbalancer + "&" + "Version" + EQUALSSIGN + SERVICE_VERSION, AWS_ELB_SERVICE, region);

        return this.executeRequest(awsPost);
    }

    private byte[] executeRequest(final HttpPost awsPost) throws AwsException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(awsPost)) {
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

    public List<Ec2Instance> describeInstanceElbStatus(List<Ec2Instance> instances, List<String> elbs) throws AwsException {
        for (String elb : elbs) {
            byte[] awsResult = executeGetInstanceState(elb);
            AwsXpathUtil.updateInstanceState(instances, awsResult);
        }
        return instances;
    }
}
