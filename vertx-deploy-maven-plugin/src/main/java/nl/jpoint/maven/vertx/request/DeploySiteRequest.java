package nl.jpoint.maven.vertx.request;

public class DeploySiteRequest extends Request {

    private static final String ENDPOINT = "/deploy/site";

    private final String basePath;

    public DeploySiteRequest(String group_id, String artifact_id, String version, String base_path) {
        super(group_id, artifact_id, version);
        this.basePath = base_path;
    }

    public String getBasePath() {
        return basePath;
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
