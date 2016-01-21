package nl.jpoint.vertx.mod.deploy.request;

import java.util.UUID;

public abstract class ModuleRequest {

    private final UUID id = UUID.randomUUID();
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String type;
    private final boolean snapshot;

    private String snapshotVersion = null;


    ModuleRequest(final String groupId, final String artifactId, final String version, final String classifier, final String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type != null ? type : "jar";
        this.snapshot = version.endsWith("-SNAPSHOT");
    }

    ModuleRequest(final String groupId, final String artifactId, final String version, final String type) {
        this(groupId, artifactId, version, null, type);
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
        return this.groupId + ":" + this.artifactId + ":" + this.version;
    }

    public String getMavenArtifactId() {
        return this.groupId + ":" + this.artifactId;
    }

    public String getRemoteLocation() {
        StringBuilder builder = new StringBuilder()
                .append(getGroupId().replaceAll("\\.", "/"))
                .append("/")
                .append(getArtifactId())
                .append("/")
                .append(getVersion())
                .append("/")
                .append(getFileName());
        return builder.toString();
    }

    public String getFileName() {
        StringBuilder builder = new StringBuilder()
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
        builder.append(".");
        builder.append(type);

        return builder.toString();

    }

    public String getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(String snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public String getMetadataLocation() {
        return getGroupId().replaceAll("\\.", "/") + "/" + getArtifactId() + "/" + getVersion() + "/" + "maven-metadata.xml";

    }

    public String getType() {
        return type;
    }

    public abstract boolean restart();


}
