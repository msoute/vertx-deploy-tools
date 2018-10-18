package nl.jpoint.vertx.deploy.agent.aws;

import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealth;

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

    public static AwsState map(TargetHealth health) {
        switch (health.getState()) {
            case "draining":
                return ENTERINGSTANDBY;
            case "unused":
                return NOTREGISTERED;
            case "unhealthy":
            case "initial":
                return OUTOFSERVICE;
            case "healthy":
                return INSERVICE;
            default:
                return UNKNOWN;
        }
    }

}
