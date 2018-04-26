package nl.jpoint.vertx.deploy.agent.util;

public enum ApplicationDeployState {
    OK,
    ERROR;

    public static ApplicationDeployState map(String state) {
        if (state == null || state.isEmpty()) {
            return OK;
        }
        try {
            return ApplicationDeployState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OK;
        }
    }
}
