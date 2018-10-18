package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint"})
public class DeployApplicationRequest extends Request {

    private static final String ENDPOINT = "/deploy/module";
    @JsonProperty
    private final boolean restart;

    public DeployApplicationRequest(String groupId, String artifactId, String version, String classifier, String type, boolean restart) {
        super(groupId, artifactId, version, classifier, type);
        this.restart = restart;
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

}

