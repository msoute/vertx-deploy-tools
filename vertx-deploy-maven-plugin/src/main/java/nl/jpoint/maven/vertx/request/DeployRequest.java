package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
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

    @JsonProperty("with_elb")
    private final boolean elb;
    @JsonProperty("with_as")
    private final boolean autoScaling;
    @JsonProperty("restart")
    private final boolean restart;
    @JsonProperty("instance_id")
    private final String instanceId;
    @JsonProperty(value = "as_group_id")
    private final String asGroupId;

    private DeployRequest(List<Request> modules, List<Request> artifacts, List<Request> configs, boolean elb, boolean restart, String instanceId, String asGroupId) {
        this.modules = modules;
        this.artifacts = artifacts;
        this.configs = configs;
        this.elb = elb;
        this.restart = restart;
        this.instanceId = instanceId;
        this.asGroupId = asGroupId;
        autoScaling = (asGroupId != null);
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

    public String toJson(boolean pretty) {
        try {
            if (pretty) {
                return writer.withDefaultPrettyPrinter().writeValueAsString(this);
            }
            return writer.writeValueAsString(this);

        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String getEndpoint() {
        return ENDPOINT;
    }

    public static class Builder {

        private List<Request> modules = new ArrayList<>();
        private List<Request> artifacts  = new ArrayList<>();
        private List<Request> configs  = new ArrayList<>();
        private boolean elb = false ;
        private boolean restart = true;
        private String autoScalingGroup = "";
        private String instanceId = "";

        public Builder withElb(final boolean elb) {
            this.elb = elb;
            return this;
        }

        public Builder withRestart(final boolean restart) {
            this.restart = restart;
            return this;
        }

        public Builder withModules(final List<Request> modules) {
            this.modules = modules;
            return this;
        }
        public Builder withArtifacts(final List<Request> artifacts) {
            this.artifacts = artifacts;
            return this;
        }
        public Builder withConfigs(final List<Request> configs) {
            this.configs = configs;
            return this;
        }

        public Builder withAutoScalingGroup(final String autoScalingGroup) {
            this.autoScalingGroup = autoScalingGroup;
            return this;
        }
        public Builder withInstanceId(final String instanceId ) {
            this.instanceId = instanceId;
            return this;
        }

        public DeployRequest build() {
            return new DeployRequest(modules, artifacts, configs, elb, restart, instanceId, autoScalingGroup);
        }
    }
}

