package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"endpoint"})
public class DeployModuleRequest extends Request {

    private static final String ENDPOINT = "/deploy/module";

    @JsonProperty
    private final int instances;
    @JsonProperty
    private final boolean restart;

    public DeployModuleRequest(String group_id, String artifact_id, String version, String type, int instances, boolean restart) {
        super(group_id, artifact_id, version, null, type);
        this.instances = instances;
        this.restart = restart;
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

}

