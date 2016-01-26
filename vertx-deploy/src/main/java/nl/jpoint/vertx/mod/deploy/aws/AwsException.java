package nl.jpoint.vertx.mod.deploy.aws;

public class AwsException extends RuntimeException {
    public AwsException(Throwable t) {
        super(t);
    }
    public AwsException(String message) {
        super(message);
    }
}
