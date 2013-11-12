package nl.soutenet.vertx.mod.cluster.request;

import java.util.UUID;

public class ModuleRequest {

    private final UUID id = UUID.randomUUID();

    private final String groupId;
    private final String artifactId;
    private final String version;

    protected ModuleRequest(final String groupId, final String artifactId, final String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
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

    public UUID getId() {
        return id;
    }

    public String getModuleId() {
        return this.groupId+"~"+this.artifactId+"~"+this.version;
    }
    public String getMavenArtifactId() {
        return this.groupId+":"+this.artifactId+":"+this.version;
    }

    public String getRemoteLocation() {
        StringBuilder builder = new StringBuilder()
                .append(getGroupId().replaceAll("\\.","/"))
                .append("/")
                .append(getArtifactId())
                .append("/")
                .append(getVersion())
                .append("/")
                .append(getArtifactId()).append("-").append(getVersion()).append(".zip");
        return builder.toString();
    }
    public boolean isAsync() {
        return false;
    }

    public boolean isSnapshot() {
        return version.endsWith("-SNAPSHOT");
    }

}
