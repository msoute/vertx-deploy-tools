package nl.soutenet.vertx.mod.cluster.request;

import org.vertx.java.core.json.JsonObject;

public class DeploySiteRequest extends ModuleRequest {

    private String context;

    private DeploySiteRequest(final String groupId, final String artifactId, final String version, final String context) {
        super(groupId, artifactId, version);
        this.context = context;
    }



    public static DeploySiteRequest fromJsonMessage(final JsonObject request) {
        return new DeploySiteRequest(request.getString("group_id"),request.getString("artifact_id"),request.getString("version"), request.getString("context"));
    }

    public String getContext() {
        return context;
    }
}
