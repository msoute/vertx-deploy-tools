package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public abstract class Request {
    private static final ObjectWriter writer = new ObjectMapper().writer();

    @JsonProperty
    private final String group_id;
    @JsonProperty
    private final String artifact_id;
    @JsonProperty
    private final String version;

    @JsonProperty
    private final String classifier;
    @JsonProperty
    private final String type;

    Request(String group_id, String artifact_id, String version, String classifier, String type) {
        this.group_id = group_id;
        this.artifact_id = artifact_id;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
    }

    public abstract String getEndpoint();
}
