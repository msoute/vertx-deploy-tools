package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint"})
public class DeployArtifactRequest extends Request {

    private static final String ENDPOINT = "/deploy/artifact";

    @JsonProperty
    private final String context;

    public DeployArtifactRequest(final String group_id, final String artifact_id, final String version, final String classifier, final String type,  final String context) {
        super(group_id, artifact_id, version, classifier, type);
        this.context = context;
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
