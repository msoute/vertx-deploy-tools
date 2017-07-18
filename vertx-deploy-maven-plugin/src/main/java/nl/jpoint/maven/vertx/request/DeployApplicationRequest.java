package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.aether.artifact.Artifact;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint", "deployType"})
public class DeployApplicationRequest extends Request {

    private static final String ENDPOINT = "/deploy/module";
    @JsonProperty
    private final boolean restart;

    public DeployApplicationRequest(Artifact artifact, boolean restart) {
        super(artifact);
        this.restart = restart;
    }

    public DeployApplicationRequest(org.apache.maven.artifact.Artifact artifact, boolean restart) {
        super(artifact);
        this.restart = restart;
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public Type getDeployType() {
        return Type.APPLICATION;
    }

}

