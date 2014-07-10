package nl.jpoint.vertx.mod.cluster.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployModuleRequest extends ModuleRequest {

    private final int instances;
    private final boolean restart;

    @JsonCreator
    public DeployModuleRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                @JsonProperty("version") final String version, @JsonProperty("instances") final int instances,
                                @JsonProperty("restart") final boolean restart) {
        super(groupId, artifactId, version);
        this.instances = instances;
        this.restart = restart;
    }

    public int getInstances() {
        return instances;
    }

    @Override
    public boolean restart() {
        return restart;
    }
}
