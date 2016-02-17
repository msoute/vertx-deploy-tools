package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class AwsContext {
    private final Region awsRegion;

    private AwsContext(final String region) {
        this.awsRegion = Region.getRegion(Regions.fromName(region));
    }

    public static AwsContext build(final String region) {
        return new AwsContext(region);
    }

    public Region getAwsRegion() {
        return awsRegion;
    }
}
