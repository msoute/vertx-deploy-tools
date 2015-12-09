package nl.jpoint.vertx.mod.deploy.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployApplicationRequest extends ModuleRequest {

    private final int instances;
    private boolean restart;

    @JsonCreator
    public DeployApplicationRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                    @JsonProperty("version") final String version, @JsonProperty("instances") final int instances,
                                    @JsonProperty("restart") final boolean restart, @JsonProperty("type") final String type) {
        super(groupId, artifactId, version, type);
        this.instances = instances;
        this.restart = restart;
    }

    public int getInstances() {
        return instances;
    }

    public void withRestart() {
        this.restart = true;
    }

    @Override
    public boolean restart() {
        return restart;
    }
}
