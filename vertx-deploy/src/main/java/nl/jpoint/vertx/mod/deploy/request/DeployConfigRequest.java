package nl.jpoint.vertx.mod.deploy.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;

public class DeployConfigRequest extends ModuleRequest {

    @JsonCreator
    private DeployConfigRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                @JsonProperty("version") final String version, @JsonProperty("classifier") final String classifier,
                                @JsonProperty("type") final String type) {
        super(groupId, artifactId, version, classifier, type);
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
