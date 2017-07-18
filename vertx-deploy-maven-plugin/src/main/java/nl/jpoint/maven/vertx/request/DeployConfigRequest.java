package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.aether.artifact.Artifact;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint", "deployType"})
public class DeployConfigRequest extends Request {

    private static final String ENDPOINT = "/deploy/config";

    public DeployConfigRequest(Artifact artifact) {
        super(artifact);
    }

    public DeployConfigRequest(org.apache.maven.artifact.Artifact artifact) {
        super(artifact);
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public Type getDeployType() {
        return Type.CONFIG;
    }
}
