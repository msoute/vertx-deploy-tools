package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.ArrayList;
import java.util.List;

public class DeployRequest {

    private static ObjectWriter writer = new ObjectMapper().writer();
    private static final String ENDPOINT = "/deploy/deploy";

    private final List<Request> modules;
    private final List<Request> artifacts;

    public DeployRequest(List<Request> modules, List<Request> artifacts) {
        this.modules = modules;
        this.artifacts = artifacts;
    }

    public List<Request> getModules() {
        return new ArrayList<>(modules);
    }

    public List<Request> getArtifacts() {
        return new ArrayList<>(artifacts);
    }

    public String toJson() {
        try {
            return writer.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getEndpoint() {
        return ENDPOINT;
    }
}
