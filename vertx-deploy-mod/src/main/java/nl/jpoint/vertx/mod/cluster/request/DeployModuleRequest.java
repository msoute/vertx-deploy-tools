package nl.jpoint.vertx.mod.cluster.request;

import org.vertx.java.core.json.JsonObject;

public class DeployModuleRequest extends ModuleRequest {

    private final int instances;

    private DeployModuleRequest(final String groupId, final String artifactId, final String version, final int instances) {
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
