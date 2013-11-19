package nl.jpoint.maven.vertx.request;

public class DeployRequest extends Request {
    private static final String ENDPOINT = "/deploy/module";

    private int instances;

    public DeployRequest(String group_id, String artifact_id, String version, int instances) {
        super(group_id, artifact_id, version);
        this.instances = instances;
    }

    public int getInstances() {
        return instances;
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}

