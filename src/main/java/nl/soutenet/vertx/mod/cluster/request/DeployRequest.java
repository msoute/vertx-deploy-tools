package nl.soutenet.vertx.mod.cluster.request;

import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

public class DeployRequest {

    private final UUID id = UUID.randomUUID();

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final int instances;


    private DeployRequest(final String groupId, final String artifactId, final String version, final int instances) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.instances = instances;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public int getInstances() {
        return instances;
    }

    public UUID getId() {
        return id;
    }

    public static DeployRequest fromJsonMessage(final JsonObject request) {
        return new DeployRequest(request.getString("group_id"),request.getString("artifact_id"),request.getString("version"), request.getInteger("instances"));
    }

    public String getModuleId() {
        return this.groupId+"~"+this.artifactId+"~"+this.version;
    }

    public boolean isAsync() {
        return false;
    }

    public boolean isSnapshot() {
        return version.endsWith("-SNAPSHOT");
    }
}
