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
    private static final String AWS_ACTION = "Action";
    private static final String EQUALSSIGN = "=";

    private final AwsUtil awsUtil;
    protected final SimpleDateFormat compressedIso8601DateFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    public AwsOpsWorksUtil(String accessKey, String secretAccessKey) {
        this.awsUtil = new AwsUtil(accessKey, secretAccessKey);
        this.compressedIso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public List<String> ListStackInstances(final String stackId) throws AwsException {

        List<String> hosts = new ArrayList<>();

        String targetHost = AWS_OPSWORKS_SERVICE + "." + "us-east-1" + ".amazonaws.com";
        String date = compressedIso8601DateFormat.format(new Date());

        StringBuilder payloadBuilder = new StringBuilder("{\"StackId\":\""+stackId+"\"}");

        Map<String, String> signedHeaders = this.createDefaultSignedHeaders(date, targetHost);

        HttpPost awsPost = awsUtil.createSignedPost(targetHost, signedHeaders, date, payloadBuilder.toString(), AWS_OPSWORKS_SERVICE, "us-east-1");

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
                        JsonNode instance = it.next();
                        if (instance.get("PublicDns") != null) {
                            hosts.add(instance.get("PublicDns").textValue());
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
