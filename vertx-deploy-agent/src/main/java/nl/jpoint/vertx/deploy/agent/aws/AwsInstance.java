package nl.jpoint.vertx.deploy.agent.aws;

public class AwsInstance {
    public enum PollVersion {
        V1,
        V2
    }

    private PollVersion version;
    private String id;

    private AwsInstance(PollVersion version, String id) {
        this.version = version;
        this.id = id;
    }

    public static AwsInstance forELB(String id) {
        return new AwsInstance(PollVersion.V1, id);
    }

    public static AwsInstance forALB(String id) {
        return new AwsInstance(PollVersion.V2, id);
    }

    public PollVersion getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }


}

