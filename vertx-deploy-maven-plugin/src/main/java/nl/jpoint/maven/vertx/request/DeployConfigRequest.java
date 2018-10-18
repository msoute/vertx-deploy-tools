package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint"})
public class DeployConfigRequest extends Request {

    private static final String ENDPOINT = "/deploy/config";

    public DeployConfigRequest(final String groupId, final String artifactId, final String version, final String classifier, final String type) {
        super(groupId, artifactId, version, classifier, type);

    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }
}
