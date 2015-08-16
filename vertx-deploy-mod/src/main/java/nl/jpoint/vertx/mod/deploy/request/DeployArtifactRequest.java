package nl.jpoint.vertx.mod.deploy.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployArtifactRequest extends ModuleRequest {

    @JsonCreator
    private DeployArtifactRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                  @JsonProperty("version") final String version, @JsonProperty("classifier") final String classifier,
                                  @JsonProperty("type") final String type) {
        super(groupId, artifactId, version, classifier, type);
    }

    @Override
    public boolean restart() {
        return false;
    }
}
