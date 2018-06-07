package nl.jpoint.vertx.deploy.agent.aws;

public enum AwsState {
    UNKNOWN,
    TERMINATING,
    OUTOFSERVICE,
    INSERVICE,
    NOTREGISTERED,
    PENDING,
    STANDBY,
    ENTERINGSTANDBY;

    public static AwsState map(String state) {
        if (state == null || state.isEmpty()) {
            return UNKNOWN;
        }
        return AwsState.valueOf(state.toUpperCase());
    }

}
