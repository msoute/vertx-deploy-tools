package nl.jpoint.maven.vertx.utils;

public class AwsException extends Exception {
    public AwsException(String e) {
        super(e);
    }

    public AwsException(Throwable e) {
        super(e);
    }
}
