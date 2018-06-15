package nl.jpoint.vertx.deploy.agent.util;

public enum DeployType {
    APPLICATION("application:"),
    ARTIFACT("artifact:"),
    DEFAULT("");

    private static final String LATEST_REQUEST_TAG = "latest:version";
    private static final String SCOPE_TAG = "scope:tst";
    private static final String EXCLUSION_TAG = "exclusions";
    private static final String PROPERTIES_TAG = "classifier:properties";

    private final String type;
    private final String prefix = "deploy";

    DeployType(String type) {
        this.type = type;
    }

    public String getLatestRequestTag() {
        return prefix + ":" + type + LATEST_REQUEST_TAG;
    }

    public String getScopeTag() {
        return prefix + ":" + type + SCOPE_TAG;
    }

    public String getExclusionTag() {
        return prefix + ":" + type + EXCLUSION_TAG;
    }

    public String getPropertiesTag() {
        return prefix + ":" + type + PROPERTIES_TAG;
    }
}
