package nl.jpoint.vertx.mod.deploy;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;


public class DeployConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AwsDeployModule.class);

    private static final String VERTX_HOME = "vertx.home";
    private static final String ARTIFACT_REPO = "artifact.repo";
    private static final String AWS_AUTH_KEY = "aws.auth.access.key";
    private static final String AWS_SECRET_AUTH_KEY = "aws.auth.secret.access.key";
    private static final String AWS_DEFAULT_REGION = "eu-west-1";
    private static final String AWS_REGISTER_MAX_DURATION = "aws.as.deregister.maxduration";
    private static final String CONFIG_LOCATION = "config.location";
    private static final String HTTP_AUTH_USER = "http.authUser";
    private static final String HTTP_AUTH_PASSWORD = "http.authPass";
    private static final String MAVEN_REPO_URI = "maven.repo.uri";

    private static final String AWS_ELB_ID = "aws.elb.loadbalancer";
    private static final String AWS_INSTANCE_ID = "aws.elb.instanceid";

    private final String vertxHome;
    private final String artifactRepo;
    private final URI nexusUrl;

    private String configLocation;
    private String awsAccessKey;
    private String awsSecretAccessKey;
    private String awsRegion;
    private String httpAuthUser;
    private String httpAuthPassword;

    private boolean awsEnabled = false;
    private boolean httpAuthentication = false;

    private String awsLoadbalancerId;
    private String awsInstanceId;

    private int awsMaxRegistrationDuration;

    private DeployConfig(String vertxHome, String artifactRepo, String nexusUrl) {
        this.vertxHome = vertxHome.endsWith("/") ? vertxHome : vertxHome + "/";
        this.artifactRepo = artifactRepo.endsWith("/") ? artifactRepo : artifactRepo + "/";
        this.nexusUrl =  URI.create(nexusUrl);

    }

    private static String validateRequiredField(String field, JsonObject config) {

        if (!config.containsKey(field) || config.getString(field).isEmpty()) {
            LOG.error("config missing config value for field {} ", field);
            throw new IllegalStateException("config missing config value for field " + field);
        }
        return (String) config.remove(field);
    }

    private static String validateField(String field, JsonObject config) {
        if (config.containsKey(field) && !config.getString(field).isEmpty()) {
            return (String) config.remove(field);
        }
        return "";
    }

    static DeployConfig fromJsonObject(JsonObject config) {
        if (config == null) {
            LOG.error("Unable to read config file");
            throw new IllegalStateException("Unable to read config file");
        }

        String vertxHome = validateRequiredField(VERTX_HOME, config);
        String artifactRepo = validateRequiredField(ARTIFACT_REPO, config);
        String mavenRepo = validateRequiredField(MAVEN_REPO_URI, config);

        DeployConfig deployconfig = new DeployConfig(vertxHome, artifactRepo, mavenRepo)
                .withConfigLocation(config)
                .withAwsConfig(config)
                .withHttpAuth(config);

        if (!config.isEmpty()) {
            config.fieldNames().forEach(s -> LOG.info("Unused variable in config '{}',", s));
        }
        return deployconfig;
    }


    private DeployConfig withConfigLocation(JsonObject config) {
        this.configLocation = config.getString(CONFIG_LOCATION, "");
        config.remove(CONFIG_LOCATION);
        return this;
    }

    private DeployConfig withAwsConfig(JsonObject config) {
        this.awsAccessKey = validateField(AWS_AUTH_KEY, config);
        this.awsSecretAccessKey = validateField(AWS_SECRET_AUTH_KEY, config);
        this.awsRegion = validateField(AWS_DEFAULT_REGION, config);
        this.awsInstanceId = validateField(AWS_ELB_ID, config);
        this.awsLoadbalancerId = validateField(AWS_INSTANCE_ID, config);

        this.awsMaxRegistrationDuration = config.getInteger(AWS_REGISTER_MAX_DURATION, 4);
        config.remove(AWS_REGISTER_MAX_DURATION);

        if (!awsAccessKey.isEmpty() && !awsSecretAccessKey.isEmpty()) {
            LOG.info("Enabled AWS support.");
            this.awsEnabled = true;
        } else {
            LOG.info("Disabled AWS support.");
        }
        return this;
    }

    private DeployConfig withHttpAuth(JsonObject config) {
        this.httpAuthUser = validateField(HTTP_AUTH_USER, config);
        this.httpAuthPassword = validateField(HTTP_AUTH_PASSWORD, config);

        if (!httpAuthUser.isEmpty() && !httpAuthPassword.isEmpty()) {
            LOG.info("Enabled http authentication.");
            this.httpAuthentication = true;
        } else {
            LOG.info("Disabled http authentication.");
        }

        return this;
    }

    public String getVertxHome() {
        return vertxHome;
    }

    public String getArtifactRepo() {
        return artifactRepo;
    }

    public URI getNexusUrl() {
        return nexusUrl;
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getAwsLoadbalancerId() {
        return awsLoadbalancerId;
    }

    public String getAwsInstanceId() {
        return awsInstanceId;
    }

    public int getAwsMaxRegistrationDuration() {
        return awsMaxRegistrationDuration;
    }

    public String getHttpAuthUser() {
        return httpAuthUser;
    }

    public String getHttpAuthPassword() {
        return httpAuthPassword;
    }

    public boolean isAwsEnabled() {
        return awsEnabled;
    }

    public boolean isHttpAuthentication() {
        return httpAuthentication;
    }
}
