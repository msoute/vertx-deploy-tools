package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({"endpoint"})
public class DeployRequest {

    private static final ObjectWriter writer = new ObjectMapper().writer();
    private static final String ENDPOINT = "/deploy/deploy";

    private final List<Request> modules;
    private final List<Request> artifacts;
    private final List<Request> configs;

    @JsonProperty("aws")
    private final boolean aws;
    @JsonProperty("restart")
    private final boolean restart;

    public DeployRequest(List<Request> modules, List<Request> artifacts, List<Request> configs, boolean aws, boolean restart) {
        this.modules = modules;
        this.artifacts = artifacts;
        this.configs = configs;
        this.aws = aws;
        this.restart = restart;
    }

    public List<Request> getModules() {
        return new ArrayList<>(modules);
    }

    public List<Request> getArtifacts() {
        return new ArrayList<>(artifacts);
    }

    public List<Request> getConfigs() {
        return new ArrayList<>(configs);
    }

    public String toJson() {
        try {
            return writer.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String getEndpoint() {
        return ENDPOINT;
    }
}
