package nl.jpoint.vertx.mod.deploy.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployRequest {
    private final UUID id = UUID.randomUUID();
    private final List<DeployApplicationRequest> modules;
    private final List<DeployConfigRequest> configs;
    private final List<DeployArtifactRequest> artifacts;
    private final boolean elb;
    private final boolean autoScaling;
    private boolean decrementDesiredCapacity = true;
    private final String autoScalingGroup;

    private final String instanceId;
    private boolean restart;
    private DeployState state;

    @JsonCreator
    public DeployRequest(@JsonProperty("modules") List<DeployApplicationRequest> modules,
                         @JsonProperty("artifacts") List<DeployArtifactRequest> artifacts,
                         @JsonProperty("configs") List<DeployConfigRequest> configs,
                         @JsonProperty("with_elb") boolean elb,
                         @JsonProperty("with_as") boolean autoScaling,
                         @JsonProperty("as_group_id") String autoScalingGroup,
                         @JsonProperty("instance_id") String instanceId,
                         @JsonProperty("restart") boolean restart) {
        this.modules = modules != null ? modules : Collections.emptyList();
        this.artifacts = artifacts != null ? artifacts : Collections.emptyList();
        this.configs = configs != null ? configs : Collections.emptyList();

        this.elb = elb;
        this.autoScaling = autoScaling;
        this.autoScalingGroup = autoScalingGroup;
        this.instanceId = instanceId;
        this.restart = restart;
    }

    public List<DeployArtifactRequest> getArtifacts() {
        return artifacts;
    }

    public List<DeployApplicationRequest> getModules() {
        return modules;
    }

    public List<DeployConfigRequest> getConfigs() {
        return configs;
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

    public boolean withElb() {
        return elb;
    }

    public boolean withAutoScaling() {
        return elb && autoScaling;
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

    public boolean isDecrementDesiredCapacity() {
        return decrementDesiredCapacity;
    }

    @JsonProperty("as_decrement_desired_capacity")
    public void setDecrementDesiredCapacity(boolean decrementDesiredCapacity) {
        this.decrementDesiredCapacity = decrementDesiredCapacity;
    }

    public void setRestart(boolean restart) {
        this.restart = restart;
    }
}
