package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint"})
public class DeployConfigRequest extends Request {

    private static final String ENDPOINT = "/deploy/config";

    public DeployConfigRequest(final String group_id, final String artifact_id, final String version, final String classifier) {
        super(group_id, artifact_id, version, classifier);

    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
