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
    private static final String AWS_ACTION_REGISTER_INSTANCE = "RegisterInstancesWithLoadBalancer";
    private static final String AWS_ACTION_DEREGISTER_INSTANCE = "DeregisterInstancesFromLoadBalancer";
    private static final String SERVICE_VERSION = "2012-06-01";
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private AwsUtil awsUtil;

    public AwsElbUtil(String accessKey, String secretAccessKey) {

        this.awsUtil = new AwsUtil(accessKey, secretAccessKey);
        compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public List<String> listLBInstanceMembers(String region, String loadbalancer) throws AwsException {
        byte[] lbInfo = executeDescribeLBRequest(region, loadbalancer);
        return AwsXpathUtil.extractInstances(lbInfo);
    }

    public void registerInstanceFromLoadbalancer(String region, String loadbalancer, String instanceId) throws AwsException {
        byte[] result = this.executeRegisterInstanceFromLoadbalancer(AWS_ACTION_REGISTER_INSTANCE, region, loadbalancer, instanceId);
    }

    public void deRegisterInstanceFromLoadbalancer(String region, String loadbalancer, String instanceId) throws AwsException {
        byte[] result = this.executeRegisterInstanceFromLoadbalancer(AWS_ACTION_DEREGISTER_INSTANCE, region, loadbalancer, instanceId);
        System.out.println(new String(result));
    }
    
    public void getInstanceState(String region, String loadbalancer, String instanceId) throws AwsException {
        byte[] result = this.executeGetInstanceState(region, loadbalancer, instanceId);
        System.out.println(new String(result));
        
    }

    private byte[] executeGetInstanceState(String region, String loadbalancer, String instanceId) throws AwsException {
        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder(AWS_ACTION).append(EQUALSSIGN)
                .append("DescribeInstanceHealth").append("&")
                .append("LoadBalancerName").append(EQUALSSIGN).append(loadbalancer).append("&")
                .append("Version").append(EQUALSSIGN).append(SERVICE_VERSION);

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_ELB_SERVICE, region);

        return this.executeRequest(awsPost);


    }


    private byte[] executeRegisterInstanceFromLoadbalancer(String action, String region, String loadbalancer, String instanceId) throws AwsException {
        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder(AWS_ACTION).append(EQUALSSIGN)
                .append(action).append("&")
                .append("Instances.member.1.InstanceId").append(EQUALSSIGN).append(instanceId).append("&")
                .append("LoadBalancerName").append(EQUALSSIGN).append(loadbalancer).append("&")
                .append("Version").append(EQUALSSIGN).append(SERVICE_VERSION);

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_ELB_SERVICE, region);

        return this.executeRequest(awsPost);

    }

    private byte[] executeDescribeLBRequest(String region, String loadbalancer) throws AwsException {

        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder(AWS_ACTION).append(EQUALSSIGN)
                .append(AWS_ACTION_DESCRIBE_LB).append("&")
                .append("LoadBalancerNames.member.1").append(EQUALSSIGN).append(loadbalancer).append("&")
                .append("Version").append(EQUALSSIGN).append(SERVICE_VERSION);

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_ELB_SERVICE, region);
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
}
