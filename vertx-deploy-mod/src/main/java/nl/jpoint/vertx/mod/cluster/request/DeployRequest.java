package nl.jpoint.vertx.mod.cluster.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployRequest {
    private final UUID id = UUID.randomUUID();
    private final List<DeployModuleRequest> modules;
    private final List<DeployArtifactRequest> artifacts;
    @JsonProperty("aws")
    private final boolean aws;
    private DeployState state;

    @JsonCreator
    public DeployRequest(@JsonProperty("modules") List<DeployModuleRequest> modules,
                         @JsonProperty("artifacts") List<DeployArtifactRequest> artifacts,
                         @JsonProperty("aws") boolean aws) {
        this.modules = modules;
        this.artifacts = artifacts;
        this.aws = aws;
    }

    public List<DeployArtifactRequest> getArtifacts() {
        return artifacts;
    }

    public List<DeployModuleRequest> getModules() {
        return modules;
    }

    public UUID getId() {
        return id;
    }

    public boolean withAws() {
        return aws;
    }

    public void setState(DeployState state) {
        this.state = state;
    }

    public DeployState getState() {
        return this.state;
    }
}
