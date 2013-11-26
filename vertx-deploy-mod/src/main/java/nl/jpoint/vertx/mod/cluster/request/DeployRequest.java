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

    @JsonCreator
    public DeployRequest(@JsonProperty("modules") List< DeployModuleRequest > modules,
                         @JsonProperty("artifacts") List<DeployArtifactRequest> artifacts) {
        this.modules = modules;
        this.artifacts = artifacts;
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
}
