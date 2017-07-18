package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.aether.artifact.Artifact;

public abstract class Request {
    public enum Type {
        APPLICATION,
        ARTIFACT,
        CONFIG,
    }

    @JsonProperty("group_id")
    private final String groupId;
    @JsonProperty("artifact_id")
    private final String artifactId;
    @JsonProperty
    private final String version;

    @JsonProperty
    private final String classifier;
    @JsonProperty
    private final String type;

    public Request(String groupId, String artifactId, String version, String classifier, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
    }

    public Request(Artifact artifact) {
        this(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getClassifier(), artifact.getExtension());
    }

    public Request(org.apache.maven.artifact.Artifact artifact) {
        this(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getClassifier(), artifact.getType());
    }

    public abstract String getEndpoint();

    public abstract Type getDeployType();

}


