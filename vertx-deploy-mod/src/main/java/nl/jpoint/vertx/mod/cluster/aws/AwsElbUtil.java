package nl.jpoint.vertx.mod.cluster.aws;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AwsElbUtil {

    public static final String EQUALSSIGN = "=";
    private static final String AWS_ELB_SERVICE = "elasticloadbalancing";
    private static final String AWS_ACTION = "Action";
    private static final String AWS_ACTION_DESCRIBE_LB = "DescribeLoadBalancers";
    private static final String SERVICE_VERSION = "2012-06-01";
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private AwsUtil awsUtil;

    public AwsElbUtil(AwsUtil awsUtil) {

        this.awsUtil = awsUtil;
        compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    }

    public void executeDescribeLBRequest(String region, String loadbalancer) throws IOException, NoSuchAlgorithmException, InvalidKeyException {

        String targetHost = AWS_ELB_SERVICE + "." + region + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder(AWS_ACTION).append(EQUALSSIGN)
                .append(AWS_ACTION_DESCRIBE_LB).append("&")
                .append("LoadBalancerNames.member.1").append(EQUALSSIGN).append(loadbalancer).append("&")
                .append("Version").append(EQUALSSIGN).append(SERVICE_VERSION);

        Map<String, String> signedHeaders = new HashMap<>();
        signedHeaders.put("X-Amz-Date", date);
        signedHeaders.put("Host", targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_ELB_SERVICE, region);

        CloseableHttpClient client = HttpClients.createDefault();

        CloseableHttpResponse response = client.execute(awsPost);

        System.out.println(EntityUtils.toString(response.getEntity()));

    }
}
