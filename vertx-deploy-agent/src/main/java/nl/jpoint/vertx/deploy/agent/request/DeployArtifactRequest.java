package nl.jpoint.vertx.deploy.agent.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployArtifactRequest extends ModuleRequest {

    @JsonCreator
    private DeployArtifactRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                  @JsonProperty("version") final String version, @JsonProperty("classifier") final String classifier,
                                  @JsonProperty("type") final String type) {
        super(groupId, artifactId, version, classifier, type);
    }

    @Override
    public boolean deleteBase() {
        return true;
    }

    @Override
    public boolean checkConfig() {
        return false;
    }

    @Override
    public String getLogName() {
        return LogConstants.DEPLOY_ARTIFACT_REQUEST;
    }

    public static DeployArtifactRequest build(String groupId, String artifactId, String version, String classifier, String type) {
        return new DeployArtifactRequest(groupId, artifactId, version, classifier, type);
    }
}
