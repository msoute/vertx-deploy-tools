package nl.jpoint.vertx.mod.deploy.request;

public enum DeployState {
    WAITING_FOR_DEREGISTER,
    DEPLOYING_CONFIGS,
    STOPPING_CONTAINER,
    DEPLOYING_ARTIFACTS,
    DEPLOYING_MODULES,
    UNKNOWN,
    FAILED,
    SUCCESS,
    WAITING_FOR_REGISTER
}
