package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployResult {


    private final List<String> success;
    private final Map<String, String> error;

    @JsonCreator
    public DeployResult(@JsonProperty("OK") List<String> success,
                        @JsonProperty("ERROR") Map<String, String> error) {
        this.success = success;
        this.error = error;
    }

    public List<String> getSuccess() {
        return success;
    }

    public Map<String, String> getError() {
        return error;
    }
}
