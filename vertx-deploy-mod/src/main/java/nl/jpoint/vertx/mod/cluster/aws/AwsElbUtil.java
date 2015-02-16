package nl.jpoint.vertx.mod.cluster.aws;


import nl.jpoint.vertx.mod.cluster.util.AwsXpathUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AwsElbUtil {

    private static final String EQUALSSIGN = "=";
    private static final String AWS_ELB_SERVICE = "elasticloadbalancing";
    private static final String AWS_ACTION = "Action";

    private static final String AWS_ACTION_DESCRIBE_LB = "DescribeLoadBalancers";
    private static final String AWS_ACTION_REGISTER_INSTANCE = "RegisterInstancesWithLoadBalancer";
    private static final String AWS_ACTION_DEREGISTER_INSTANCE = "DeregisterInstancesFromLoadBalancer";
    private static final String SERVICE_VERSION = "2012-06-01";
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private final String region;
    private final String loadbalancer;
    private final AwsUtil awsUtil;
    private final String instanceId;

    public AwsElbUtil(String accessKey, String secretAccessKey, String region, String loadbalancer, String instanceId) {
        this(new AwsUtil(accessKey, secretAccessKey), region, loadbalancer, instanceId);
    }

    public AwsElbUtil(AwsUtil awsUtil, String region, String loadbalancer, String instanceId) {
        this.region = region;
        this.loadbalancer = loadbalancer;
        this.instanceId = instanceId;

        this.awsUtil = awsUtil;
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public AwsElbUtil(AwsContext context, String region) {
        this.region = region;
        this.loadbalancer = null;
        this.instanceId = null;
        this.awsUtil = context.getAwsUtil();
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public List<String> listLBInstanceMembers() throws AwsException {
        byte[] lbInfo = executeDescribeLBRequest();
        return AwsXpathUtil.listInstances(lbInfo);
    }

    public boolean registerInstanceWithLoadbalancer() throws AwsException {
        byte[] result = this.executeRegisterInstanceFromLoadbalancer(AWS_ACTION_REGISTER_INSTANCE);
        List<String> members = AwsXpathUtil.listInstances(result);
        return members.contains(instanceId);
    }

    public boolean deRegisterInstanceFromLoadbalancer() throws AwsException {
        byte[] result = this.executeRegisterInstanceFromLoadbalancer(AWS_ACTION_DEREGISTER_INSTANCE);
        List<String> members = AwsXpathUtil.listInstances(result);
        return members.contains(instanceId);
    }
    
    public AwsState getInstanceState() throws AwsException {
        byte[] result = this.executeGetInstanceState();
        return AwsState.valueOf(AwsXpathUtil.instanceState(result, forInstanceId()).toUpperCase());
    }

    public AwsState getInstanceState(final String instanceId, final String loadbalancer) throws AwsException {
        byte[] result = this.executeGetInstanceState(loadbalancer);
        return AwsState.valueOf(AwsXpathUtil.instanceState(result, instanceId).toUpperCase());
    }

    private byte[] executeGetInstanceState() throws AwsException {
        return executeGetInstanceState(forLoadbalancer());
    }

    private byte[] executeGetInstanceState(final String loadbalancer) throws AwsException {
        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, AWS_ACTION + EQUALSSIGN + "DescribeInstanceHealth" + "&" + "LoadBalancerName" + EQUALSSIGN + loadbalancer + "&" + "Version" + EQUALSSIGN + SERVICE_VERSION, AWS_ELB_SERVICE, region);

        return this.executeRequest(awsPost);
    }

    private byte[] executeRegisterInstanceFromLoadbalancer(String action) throws AwsException {
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

    private byte[] executeDescribeLBRequest() throws AwsException {

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

    public String forInstanceId() {
        return instanceId;
    }

    public String forLoadbalancer() {
        return loadbalancer;
    }

    public String forRegion() {
        return region;
    }
}
