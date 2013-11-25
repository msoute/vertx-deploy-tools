package nl.jpoint.vertx.mod.cluster.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.vertx.java.core.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployModuleRequest extends ModuleRequest {

    private final int instances;

    @JsonCreator
    private DeployModuleRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                @JsonProperty("version") final String version, @JsonProperty("instances")  final int instances) {
        super(groupId, artifactId, version);
        this.instances = instances;
    }

    public int getInstances() {
        return instances;
    }

    public static DeployModuleRequest fromJsonMessage(final JsonObject request) {
        return new DeployModuleRequest(request.getString("group_id"), request.getString("artifact_id"), request.getString("version"), request.getInteger("instances"));
    }


}
