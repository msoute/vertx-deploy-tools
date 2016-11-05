package nl.jpoint.vertx.mod.deploy.util;

public final class LogConstants {
    public static final String ERROR_EXECUTING_REQUEST = "Error executing request {}.";
    public static final String REQUEST_ALREADY_REGISTERED = "[{} - {}]: Request already registered.";
    public static final String REQUEST_NOT_REGISTERED = "[{} - {}]: Request not registered.";


    public static final String INVOKE_CONTAINER = "InvokeContainer";
    public static final String CONSOLE_COMMAND = "ConsoleCommand";
    public static final String CLUSTER_MANAGER = "ClusterManager";
    public static final String DEPLOY_REQUEST = "DeployRequest";
    public static final String DEPLOY_CONFIG_REQUEST = "DeployConfigRequest";
    public static final String DEPLOY_ARTIFACT_REQUEST = "DeployArtifactRequest";
    public static final String AWS_ELB_REQUEST = "ConfigureAwsElb";
    public static final String AWS_AS_REQUEST = "ConfigureAwsAutoScaling";
    public static final String STARTUP = "Startup";

    private LogConstants() {
        // hide
    }
}
