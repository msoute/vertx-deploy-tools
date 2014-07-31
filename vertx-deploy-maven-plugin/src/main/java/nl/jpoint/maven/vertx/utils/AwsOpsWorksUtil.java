package nl.jpoint.maven.vertx.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AwsOpsWorksUtil {

    private final static String AWS_OPSWORKS_SERVICE = "opsworks";
    private final String targetHost = AWS_OPSWORKS_SERVICE + "." + "us-east-1" + ".amazonaws.com";

    private final AwsUtil awsUtil;
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    public AwsOpsWorksUtil(String accessKey, String secretAccessKey) {
        this.awsUtil = new AwsUtil(accessKey, secretAccessKey);
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private String getElasticIp(String stackId, String instanceId) throws AwsException {
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder("{\"InstanceId\":\""+instanceId+"\"}");

        //StringBuilder payloadBuilder = new StringBuilder("{\"InstanceId\":\""+instanceId+"\",\"StackId\":\""+stackId+"\"}");

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_OPSWORKS_SERVICE, "us-east-1", "DescribeElasticIps");

        byte[] result = this.executeRequest(awsPost);
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser = null;
        try {
            parser = factory.createParser(result);
            JsonNode describeResult = mapper.readValue(parser, JsonNode.class);
            if (describeResult != null) {
                JsonNode eips = describeResult.get("ElasticIps");
                if (eips != null) {
                    Iterator<JsonNode> it = eips.elements();
                    while (it.hasNext()) {
                        JsonNode eip = it.next();
                        if (instanceId.equals(eip.get("InstanceId").textValue())) {
                            return eip.get("Ip").textValue();
                        }
                    }
                }
            }
        } catch (IOException e) {
           throw new AwsException(e);
        }
        return null;

    }

    public List<String> ListStackInstances(final String stackId, boolean usePrivateIp) throws AwsException {

        List<String> hosts = new ArrayList<>();

        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder("{\"StackId\":\""+stackId+"\"}");

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_OPSWORKS_SERVICE, "us-east-1", "DescribeInstances");

        byte[] result = this.executeRequest(awsPost);
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        try {
            JsonParser parser = factory.createParser(result);
            JsonNode describeResult = mapper.readValue(parser, JsonNode.class);
            if (describeResult != null) {
                JsonNode instances = describeResult.get("Instances");
                if (instances != null) {
                    Iterator<JsonNode> it = instances.elements();
                    while (it.hasNext()) {
                        String host = null;
                        JsonNode instance = it.next();
                        if (usePrivateIp) {
                            host = instance.get("PrivateIp").textValue();
                        } else {
                            if (instance.get("InstanceId") != null) {
                                host = getElasticIp(stackId, instance.get("InstanceId").textValue());
                            }
                            if (host == null && instance.get("PublicDns") != null) {
                                host = instance.get("PublicDns").textValue();

                            }
                        }
                        if (host != null) {
                            hosts.add(host);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new AwsException(e);
        }

        return hosts;

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
