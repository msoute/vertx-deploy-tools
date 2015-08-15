package nl.jpoint.vertx.mod.deploy.aws;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class AwsUtil {

    private static final List<String> ignoreUpperCase = new ArrayList<>(Collections.singletonList("x-amz-date"));
    private static final String EOL = "\n";
    private static final String COLON = ":";
    private static final String SEMICOLON = ";";
    private static final String ALGORITHM = "SHA-256";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String AWS_SIGN_ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String UTF_8 = "UTF-8";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private final String awsAccessKey;
    private final String awsSecretAccessKey;


    public AwsUtil(final String awsAccessKey, final String awsSecretAccessKey) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretAccessKey = awsSecretAccessKey;
    }

    HttpGet createSignedGet(String targetHost, Map<String, String> queryParameters, Map<String, String> signedHeaders, String date, String service, String region, String action) throws AwsException {
        HttpGet awsGet;
        try {
            String canonicalRequest = this.createCanonicalRequest(GET, createURLEncodedCanonicalQueryString(queryParameters), signedHeaders, "");
            String signString = this.createSignString(date, region, service, canonicalRequest);
            String signature = this.createSignature(date, region, service, signString);
            String authorization = this.createAuthorizationHeaderValue(date, region, service, signedHeaders, signature);

            awsGet = new HttpGet("https://" + targetHost + "/?" + createQueryString(queryParameters));

            awsGet.addHeader("X-Amz-Date", date);
            awsGet.addHeader("Authorization", authorization);

        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
            throw new AwsException(e);
        }
        return awsGet;
    }

    HttpPost createSignedPost(String targetHost, Map<String, String> signedHeaders, String date, String payload, String service, String region) throws AwsException {
        HttpPost awsPost;
        try {
            String canonicalRequest = this.createCanonicalRequest(POST, null, signedHeaders, payload);
            String signString = this.createSignString(date, region, service, canonicalRequest);
            String signature = this.createSignature(date, region, service, signString);
            String authorization = this.createAuthorizationHeaderValue(date, region, service, signedHeaders, signature);

            awsPost = new HttpPost("https://" + targetHost);
            awsPost.addHeader("Host", targetHost);
            awsPost.addHeader("X-Amz-Date", date);
            // awsPost.addHeader("X-Amz-Target", "OpsWorks_20130218." + action);
            awsPost.addHeader("Authorization", authorization);
            awsPost.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            //   awsPost.addHeader("Content-Type", "application/x-amz-json-1.1");

            ByteArrayInputStream bos = new ByteArrayInputStream(payload.getBytes());
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(bos);
            entity.setContentLength(payload.getBytes().length);
            awsPost.setEntity(entity);
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
            throw new AwsException(e);
        }
        return awsPost;

    }

    private String createAuthorizationHeaderValue(String date, String region, String service, Map<String, String> headers, String signature) {
        return AWS_SIGN_ALGORITHM + " " + "Credential=" + awsAccessKey + "/" + toShortDate(date) + "/" + region + "/" + service + "/" + "aws4_request" + ", " + "SignedHeaders=" + createSignedHeadersString(toSortedMap(headers)) + ", " + "Signature=" + signature;
    }

    private String createSignature(String date, String region, String service, String signString) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        return Hex.encodeHexString(hmac(deriveSigningKey(date, region, service), signString));
    }

    private String createSignString(String date, String region, String service, String canonicalRequest) {
        return AWS_SIGN_ALGORITHM + EOL + date + EOL + toShortDate(date) + "/" + region + "/" + service + "/" + "aws4_request" + EOL + hash(canonicalRequest);
    }

    private String createCanonicalRequest(String method, String canonicalQueryString, Map<String, String> headers, String payload) {

        SortedMap<String, String> sortedHeaders = toSortedMap(headers);

        StringBuilder signedHeaderString = new StringBuilder();
        StringBuilder builder = new StringBuilder(method).append(EOL)
                .append("/").append(EOL);
        if (GET.equals(method) && canonicalQueryString != null && !canonicalQueryString.isEmpty()) {
            builder.append(canonicalQueryString).append(EOL);
        } else {
            builder.append("").append(EOL);
        }
        Iterator<String> it = sortedHeaders.keySet().iterator();

        while (it.hasNext()) {
            String key = it.next();
            String value;
            if (ignoreUpperCase.contains(key.toLowerCase())) {
                value = sortedHeaders.get(key);
            } else {
                value = sortedHeaders.get(key).toLowerCase();
            }
            builder.append(key.toLowerCase()).
                    append(COLON).
                    append(value).
                    append(EOL);

            signedHeaderString.append(key.toLowerCase());
            if (it.hasNext()) {
                signedHeaderString.append(SEMICOLON);
            }

        }
        builder.append("").append(EOL)
                .append(signedHeaderString.toString()).append(EOL);
        if (payload != null) {
            builder.append(hash(payload));
        } else {
            builder.append("");
        }
        return builder.toString();
    }

    private byte[] deriveSigningKey(String date, String region, String service) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        return hmac(hmac(hmac(hmac(("AWS4" + awsSecretAccessKey).getBytes(UTF_8), toShortDate(date)), region), service), "aws4_request");
    }

    private byte[] hmac(byte[] key, String msg) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(keySpec);
        return mac.doFinal(msg.getBytes(UTF_8));


    }

    private String hash(String toSign) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(toSign.getBytes(UTF_8));
            byte[] digest = md.digest();
            return Hex.encodeHexString(digest).toLowerCase();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return null;
        }

    }

    private String toShortDate(String date) {
        return date.substring(0, date.indexOf('T'));
    }

    private SortedMap<String, String> toSortedMap(Map<String, String> toSort) {

        SortedMap<String, String> sortedMap = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });
        sortedMap.putAll(toSort);
        return sortedMap;
    }

    private String createSignedHeadersString(SortedMap<String, String> sortedHeaders) {
        Iterator<String> it = sortedHeaders.keySet().iterator();
        StringBuilder signedHeadersString = new StringBuilder();

        while (it.hasNext()) {
            signedHeadersString.append(it.next());
            if (it.hasNext()) {
                signedHeadersString.append(SEMICOLON);
            }
        }

        return signedHeadersString.toString();
    }

    // Works for now
    private String createURLEncodedCanonicalQueryString(Map<String, String> queryParameters) throws AwsException {

        List<String> keyList = new ArrayList<>();
        keyList.addAll(queryParameters.keySet());

        // Sort list
        Collections.sort(keyList);
        StringBuilder queryStringBuilder = new StringBuilder();
        Iterator<String> queryIt = keyList.iterator();
        while (queryIt.hasNext()) {
            String key = queryIt.next();

            queryStringBuilder.append(key).append("=").append(queryParameters.get(key));
            if (queryIt.hasNext()) {
                queryStringBuilder.append("&");
            }

        }
        return queryStringBuilder.toString();
    }

    private String createQueryString(Map<String, String> queryParameters) throws AwsException {

        List<String> keyList = new ArrayList<>();
        keyList.addAll(queryParameters.keySet());

        // Sort list
        Collections.sort(keyList);
        StringBuilder queryStringBuilder = new StringBuilder();
        Iterator<String> queryIt = keyList.iterator();
        while (queryIt.hasNext()) {
            String key = queryIt.next();

            queryStringBuilder.append(key).append("=").append(queryParameters.get(key));
            if (queryIt.hasNext()) {
                queryStringBuilder.append("&");
            }

        }
        return queryStringBuilder.toString();
    }


}
