package nl.jpoint.vertx.mod.cluster.aws;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AwsElbUtil {

    public static final String EQUALSSIGN = "=";
    private static final String AWS_ELB_SERVICE = "elasticloadbalancing";
    private static final String AWS_ACTION = "Action";
    private static final String AWS_ACTION_DESCRIBE_LB = "DescribeLoadBalancers";
    private static final String AWS_ACTION_DEREGISTER_INSTANCE = "DeregisterInstancesFromLoadBalancer";
    private static final String SERVICE_VERSION = "2012-06-01";
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private AwsUtil awsUtil;

    public AwsElbUtil(AwsUtil awsUtil) {

        this.awsUtil = awsUtil;
        compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    }

    public void listLBInstanceMembers(String region, String loadbalancer) throws AwsException {
        byte[] lbInfo = executeDescribeLBRequest(region, loadbalancer);
        List<String> instances = AwsXpathUtil.extractInstances(lbInfo);
    }

    /**
     * @param region
     * @param loadbalancer
     * @param instanceId
     */
    public void deregisterInstanceFromLoadbalancer(String region, String loadbalancer, String instanceId) throws AwsException {
        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder(AWS_ACTION).append(EQUALSSIGN)
                .append(AWS_ACTION_DEREGISTER_INSTANCE).append("&")
                .append("Instances.member.1.InstanceId").append(EQUALSSIGN).append(instanceId).append("&")
                .append("LoadBalancerName").append(EQUALSSIGN).append(loadbalancer).append("&")
                .append("Version").append(EQUALSSIGN).append(SERVICE_VERSION);
        System.out.println(payloadBuilder.toString());

        Map<String, String> signedHeaders = new HashMap<>();
        signedHeaders.put("X-Amz-Date", date);
        signedHeaders.put("Host", targetHost);

        try {
            HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_ELB_SERVICE, region);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(awsPost)) {
                    System.out.println(EntityUtils.toString(response.getEntity()));
                    //return EntityUtils.toByteArray(response.getEntity());
                }
            }
        } catch (IOException e) {
            throw new AwsException(e);
        }

    }

    private byte[] executeDescribeLBRequest(String region, String loadbalancer) throws AwsException {

        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder(AWS_ACTION).append(EQUALSSIGN)
                .append(AWS_ACTION_DESCRIBE_LB).append("&")
                .append("LoadBalancerNames.member.1").append(EQUALSSIGN).append(loadbalancer).append("&")
                .append("Version").append(EQUALSSIGN).append(SERVICE_VERSION);

        Map<String, String> signedHeaders = new HashMap<>();
        signedHeaders.put("X-Amz-Date", date);
        signedHeaders.put("Host", targetHost);

        try {
            HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_ELB_SERVICE, region);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(awsPost)) {
                    return EntityUtils.toByteArray(response.getEntity());

                }
            }
        } catch (IOException e) {
            throw new AwsException(e);
        }
    }
}
