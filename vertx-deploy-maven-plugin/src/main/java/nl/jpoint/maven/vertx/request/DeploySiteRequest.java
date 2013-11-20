package nl.jpoint.maven.vertx.request;

public class DeploySiteRequest extends Request {

    private static final String ENDPOINT = "/deploy/site";

    private final String basePath;

    public DeploySiteRequest(final String group_id, final String artifact_id, final String version, final String classifier, final String base_path) {
        super(group_id, artifact_id, version, classifier);
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
