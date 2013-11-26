package nl.jpoint.maven.vertx.request;

public class DeployModuleRequest extends Request {
    private static final String ENDPOINT = "/deploy/module";

    private final int instances;

    public DeployModuleRequest(String group_id, String artifact_id, String version, int instances) {
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

