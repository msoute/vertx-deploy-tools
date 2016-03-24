package nl.jpoint.vertx.mod.deploy.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployApplicationRequest extends ModuleRequest {

    private boolean running = false;
    private String javaOpts = "";
    private String instances = "1";
    private String configLocation = "";
    private boolean installed = false;

    @JsonCreator
    public DeployApplicationRequest(@JsonProperty("group_id") final String groupId, @JsonProperty("artifact_id") final String artifactId,
                                    @JsonProperty("version") final String version,@JsonProperty("classifier") final String classifier, @JsonProperty("type") final String type) {
        super(groupId, artifactId, version, classifier, type);
    }

    @Override
    public boolean deleteBase() {
        return false;
    }

    @Override
    public boolean checkConfig() {
        return false;
    }

    @Override
    public String getLogName() {
        return LogConstants.DEPLOY_ARTIFACT_REQUEST;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void withJavaOpts(String javaOpts) {
        this.javaOpts = javaOpts != null ? javaOpts : "";
    }

    public void withConfigLocation(String configLocation) {
        this.configLocation = configLocation != null ? configLocation : "";
    }

    public void withInstances(String instances) {
        this.instances = instances;
    }

    public String getJavaOpts() {
        return javaOpts;
    }

    public String getInstances() {
        return instances;
    }

    public String getConfigLocation() {
        return this.configLocation;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public static DeployApplicationRequest build(String groupId, String artifactId, String version, String classifier) {
        return new DeployApplicationRequest(groupId, artifactId, version, classifier, "jar");
    }
}
