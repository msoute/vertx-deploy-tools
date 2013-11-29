package nl.jpoint.vertx.mod.cluster.request;

import java.util.UUID;

public class ModuleRequest {

    private final UUID id = UUID.randomUUID();
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;

    private String snapshotVersion = null;

    ModuleRequest(final String groupId, final String artifactId, final String version, final String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
    }

    ModuleRequest(final String groupId, final String artifactId, final String version) {
        this(groupId, artifactId, version, null);
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
        return this.groupId + "~" + this.artifactId + "~" + this.version;
    }

    public String getMavenArtifactId() {
        return this.groupId + ":" + this.artifactId + ":" + this.version;
    }

    public String getRemoteLocation() {
        StringBuilder builder = new StringBuilder()
                .append(getGroupId().replaceAll("\\.", "/"))
                .append("/")
                .append(getArtifactId())
                .append("/")
                .append(getVersion())
                .append("/")
                .append(getArtifactId()).append("-");

        if (snapshotVersion != null) {
            builder.append(getSnapshotVersion());
        } else {
            builder.append(getVersion());
        }
        if (classifier != null && !classifier.isEmpty()) {
            builder.append("-")
                    .append(classifier);
        }
        builder.append(".zip");
        return builder.toString();
    }

    public String getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(String snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public boolean isAsync() {
        return false;
    }

    public boolean isSnapshot() {
        return version.endsWith("-SNAPSHOT");
    }

    public String getMetadataLocation() {
        return getGroupId().replaceAll("\\.", "/") + "/" + getArtifactId() + "/" + getVersion() + "/" + "maven-metadata.xml";

    }


}
