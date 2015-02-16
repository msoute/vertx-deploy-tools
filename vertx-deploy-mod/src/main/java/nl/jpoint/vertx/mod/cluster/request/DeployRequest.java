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
    private final boolean aws;
    private final boolean autoscaling;
    private final String autoScalingGroup;
    private final String instanceId;

    private final boolean restart;
    private DeployState state;

    @JsonCreator
    public DeployRequest(@JsonProperty("modules") List<DeployModuleRequest> modules,
                         @JsonProperty("artifacts") List<DeployArtifactRequest> artifacts,
                         @JsonProperty("aws") boolean aws,
                         @JsonProperty("autoscaling") boolean autoscaling,
                         @JsonProperty("as_group_id") String autoScalingGroup,
                         @JsonProperty("instance_id") String instanceId,
                         @JsonProperty("restart") boolean restart) {
        this.modules = modules;
        this.artifacts = artifacts;
        this.aws = aws;
        this.autoscaling = autoscaling;
        this.autoScalingGroup = autoScalingGroup;
        this.instanceId = instanceId;
        this.restart = restart;
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

    public String getAutoScalingGroup() {
        return autoScalingGroup;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public boolean withAws() {
        return aws;
    }
    public boolean withAutoscaling() {
        return aws && autoscaling;
    }

    public boolean withRestart() {
        return restart;
    }

    public void setState(DeployState state) {
        this.state = state;
    }

    public DeployState getState() {
        return this.state;
    }
}
