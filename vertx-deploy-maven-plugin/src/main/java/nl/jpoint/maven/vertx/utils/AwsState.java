package nl.jpoint.maven.vertx.utils;

public enum AwsState {
    UNKNOWN,
    TERMINATING,
    NOTREGISTERED,
    OUTOFSERVICE,
    ENTERINGSTANDBY,
    STANDBY,
    PENDING,
    INSERVICE;

    public static AwsState map(String state) {
        if (state == null || state.isEmpty()) {
            return UNKNOWN;
        }
        return AwsState.valueOf(state.toUpperCase());
    }
}
