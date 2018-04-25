package nl.jpoint.maven.vertx.mojo;

public class MetricsConfiguration {


    private String namespace;
    private String application;
    private String environment;

    private MetricsConfiguration(String namespace, String application, String environment) {
        this.namespace = namespace;
        this.application = application;
        this.environment = environment;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getApplication() {
        return application;
    }

    public String getEnvironment() {
        return environment;
    }

    public static MetricsConfiguration buildMetricsConfiguration(String namespace, String application, String environment) {

        if (namespace == null || application == null || namespace.isEmpty() || application.isEmpty()) {
            return null;
        }
        return new MetricsConfiguration(namespace, application, environment != null ? environment : "");
    }
}
