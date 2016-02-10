package nl.jpoint.maven.vertx.utils;

public enum AwsState {
    UNKNOWN,
    TERMINATING,
    NOTREGISTERED,
    OUTOFSERVICE,
    ENTERINGSTANDBY,
    STANDBY,
    PENDING,
    RUNNING,
    INSERVICE;

    public static AwsState map(String state) {
        if (state == null || state.isEmpty()) {
            return UNKNOWN;
        }
        try {
            return AwsState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
