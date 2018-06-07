package nl.jpoint.vertx.deploy.agent.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;

public class DeployConfigRequest extends ModuleRequest {

    @JsonCreator
    private DeployConfigRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                @JsonProperty("version") final String version, @JsonProperty("classifier") final String classifier,
                                @JsonProperty("type") final String type) {
        super(groupId, artifactId, version, classifier, type);
    }

    public static DeployConfigRequest build(String groupId, String artifactId, String version, String classifier) {
        return new DeployConfigRequest(groupId, artifactId, version, classifier, "config");
    }

    @Override
    public boolean deleteBase() {
        return false;
    }

    @Override
    public boolean checkConfig() {
        return true;
    }

    @Override
    public String getLogName() {
        return LogConstants.DEPLOY_CONFIG_REQUEST;
    }
}
