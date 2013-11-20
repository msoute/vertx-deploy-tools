package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public abstract class Request {
    private static ObjectWriter writer = new ObjectMapper().writer();

    protected String group_id;
    protected String artifact_id;
    protected String version;

    protected String classifier;

    public Request(String group_id, String artifact_id, String version, String classifier) {
        this.group_id = group_id;
        this.artifact_id = artifact_id;
        this.version = version;
        this.classifier = classifier;
    }

    public Request(String group_id, String artifact_id, String version) {
      this(group_id,artifact_id,version, null);
    }


    public String getGroup_id() {
        return group_id;
    }

    public String getArtifact_id() {
        return artifact_id;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String toJson() {
        try {
            return writer.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public abstract String getEndpoint();
}
