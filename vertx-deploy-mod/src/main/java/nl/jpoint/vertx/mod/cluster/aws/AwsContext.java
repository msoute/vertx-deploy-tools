package nl.jpoint.vertx.mod.cluster.aws;


public class AwsContext {
    private final AwsUtil awsUtil;
    private final String region;

    private AwsContext(final String accesKey, final String secretAccessKey, final String region) {
        this.region = region;
        this.awsUtil = new AwsUtil(accesKey, secretAccessKey);
    }

    public static  AwsContext build(final String accessKey, final String secretAccessKey, final String region) {
        return new AwsContext(accessKey, secretAccessKey, region);
    }

    public String getRegion() {
        return this.region;
    }

    public AwsUtil getAwsUtil() {
        return this.awsUtil;
    }
}
