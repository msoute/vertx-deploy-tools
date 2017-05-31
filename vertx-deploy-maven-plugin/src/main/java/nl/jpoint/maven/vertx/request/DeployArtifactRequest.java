package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.aether.artifact.Artifact;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint"})
public class DeployArtifactRequest extends Request {

    private static final String ENDPOINT = "/deploy/artifact";

    public DeployArtifactRequest(Artifact artifact) {
        super(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getClassifier(), artifact.getExtension());
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
