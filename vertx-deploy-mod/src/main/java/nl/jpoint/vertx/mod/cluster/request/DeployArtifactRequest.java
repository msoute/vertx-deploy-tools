package nl.jpoint.vertx.mod.cluster.request;

import org.vertx.java.core.json.JsonObject;

public class DeployArtifactRequest extends ModuleRequest {

    private String context;

    private DeployArtifactRequest(final String groupId, final String artifactId, final String version, final String classifier, final String context) {
        super(groupId, artifactId, version, classifier);
        this.context = context;
    }


    public static DeployArtifactRequest fromJsonMessage(final JsonObject request) {
        return new DeployArtifactRequest(request.getString("group_id"), request.getString("artifact_id"), request.getString("version"),request.getString("classifier"), request.getString("context"));
    }

    public String getContext() {
        return context;
    }
}
