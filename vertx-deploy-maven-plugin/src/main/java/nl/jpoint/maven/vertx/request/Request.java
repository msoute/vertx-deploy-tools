package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Request {
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

    Request(String groupId, String artifactId, String version, String classifier, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
    }

    public abstract String getEndpoint();
}
