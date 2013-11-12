package nl.soutenet.vertx.mod.cluster.request;

import org.vertx.java.core.json.JsonObject;

public class DeployRequest extends ModuleRequest {

    private final int instances;

    private DeployRequest(final String groupId, final String artifactId, final String version, final int instances) {
        super(groupId, artifactId, version);
        this.instances = instances;
    }

    public int getInstances() {
        return instances;
    }

    public static DeployRequest fromJsonMessage(final JsonObject request) {
        return new DeployRequest(request.getString("group_id"),request.getString("artifact_id"),request.getString("version"), request.getInteger("instances"));
    }


}
