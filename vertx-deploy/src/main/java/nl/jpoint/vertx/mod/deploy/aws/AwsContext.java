package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class AwsContext {
    private final AWSCredentials credentials;
    private final Region awsRegion;

    private AwsContext(final String accesKey, final String secretAccessKey, final String region) {
        this.credentials = new BasicAWSCredentials(accesKey, secretAccessKey);
        this.awsRegion = Region.getRegion(Regions.fromName(region));
    }

    public static AwsContext build(final String accessKey, final String secretAccessKey, final String region) {

        if (accessKey != null && !accessKey.isEmpty() && (secretAccessKey == null || secretAccessKey.isEmpty())) {
            throw new IllegalStateException("Missing aws key config");
        }
        return new AwsContext(accessKey, secretAccessKey, region);
    }

    public Region getAwsRegion() {
        return awsRegion;
    }

    public AWSCredentials getCredentials() {
        return credentials;
    }
}
