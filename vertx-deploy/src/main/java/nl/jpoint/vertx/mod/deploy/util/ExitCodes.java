package nl.jpoint.vertx.mod.deploy.util;

public enum ExitCodes {
    ZERO("Normal startup"),
    ONE("Vertx Initialization Issue"),
    TWO("Process Issue"),
    THREE("System Configuration Issue"),
    FOUR(""),
    FIVE("Vertx Deployment Issue");

    private final String explanation;

    ExitCodes(String explanation) {

        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return explanation;
    }

}
