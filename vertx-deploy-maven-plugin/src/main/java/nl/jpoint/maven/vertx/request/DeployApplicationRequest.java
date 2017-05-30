package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.aether.artifact.Artifact;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint"})
public class DeployApplicationRequest extends Request {

    private static final String ENDPOINT = "/deploy/module";
    @JsonProperty
    private final boolean restart;

    public DeployApplicationRequest(Artifact artifact, boolean restart) {
        super(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier(), artifact.getExtension());
        this.restart = restart;
    }

    public DeployApplicationRequest(String group_id, String artifact_id, String version, String classifier, String type, boolean restart) {
        super(group_id, artifact_id, version, classifier, type);
        this.restart = restart;
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

}

