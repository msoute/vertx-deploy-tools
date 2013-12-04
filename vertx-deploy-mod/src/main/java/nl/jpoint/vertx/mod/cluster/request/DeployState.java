package nl.jpoint.vertx.mod.cluster.request;

public enum DeployState {
    WAITING_FOR_DEREGISTER,
    DEPLOYING_MODULES,
    DEPLOYING_ARTIFACTS,
    UNKNOWN,
    FAILED,
    SUCCESS;
}
