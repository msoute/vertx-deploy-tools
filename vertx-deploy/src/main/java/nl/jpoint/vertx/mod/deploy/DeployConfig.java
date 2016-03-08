package nl.jpoint.vertx.mod.deploy;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;


public class DeployConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AwsDeployApplication.class);

    //   private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven/";

    private static final String VERTX_HOME = "vertx.home";
    private static final String RUN_DIR = "vertx.run";
    private static final String ARTIFACT_REPO = "artifact.storage";
    private static final String AWS_ENABLED = "aws.enable";
    private static final String AWS_REGION = "aws.region";
    private static final String AWS_DEFAULT_REGION = "eu-west-1";
    private static final String AWS_REGISTER_MAX_DURATION = "aws.as.register.maxduration";
    private static final String CONFIG_LOCATION = "config.location";
    private static final String HTTP_AUTH_USER = "http.authUser";
    private static final String HTTP_PORT = "http.port";
    private static final String HTTP_AUTH_PASSWORD = "http.authPass";
    private static final String MAVEN_REPO_URI = "maven.repo.uri";
    private static final String MAVEN_SNAPSHOT_POLICY = "maven.repo.snapshot.policy";
    private static final String CLUSTER = "vertx.clustering";
    private static final String DEFAULT_JAVA_OPTS = "vertx.default.java.opts";

    private static final String AUTH_TOKEN = "auth.token";

    private static final String AWS_ELB_ID = "aws.elb.loadbalancer";
    private static final String AWS_INSTANCE_ID = "aws.elb.instanceid";
    private static final String STAT_FILE = ".initial";

    private final Path vertxHome;
    private final Path artifactRepo;
    private final URI nexusUrl;
    private String configLocation;
    private String awsRegion;
    private Integer httpPort;
    private String httpAuthUser;
    private String httpAuthPassword;
    private boolean awsEnabled = false;
    private boolean httpAuthentication = false;
    private boolean mavenRemote = true;
    private String awsLoadbalancerId;
    private int awsMaxRegistrationDuration;
    private String authToken;
    private boolean asCluster = true;
    private String remoteRepoPolicy;
    private String defaultJavaOpts;
    private String runDir;
    private String statFile;

    private DeployConfig(String vertxHome, String artifactRepo, String nexusUrl) {
        this.vertxHome = Paths.get(vertxHome);
        this.artifactRepo = Paths.get(artifactRepo);
        if (nexusUrl == null || nexusUrl.isEmpty()) {
            this.mavenRemote = false;
            this.nexusUrl = null;
        } else {
            this.mavenRemote = true;
            this.nexusUrl = URI.create(nexusUrl);
        }

    }

    private static String validateRequiredField(String field, JsonObject config) {

        if (!config.containsKey(field) || config.getString(field).isEmpty()) {
            LOG.error("config missing config value for field {} ", field);
            throw new IllegalStateException("config missing config value for field " + field);
        }
        return (String) config.remove(field);
    }

    private static <T> T validateField(String field, JsonObject config) {
        return validateField(field, config, null);
    }

    private static <T> T validateField(String field, JsonObject config, T defaultValue) {
        if (config.containsKey(field) && config.getValue(field) != null) {
            return (T) config.remove(field);
        }
        return defaultValue;
    }

    static DeployConfig fromJsonObject(JsonObject config) {
        if (config == null) {
            LOG.error("Unable to read config file");
            throw new IllegalStateException("Unable to read config file");
        }

        String vertxHome = validateRequiredField(VERTX_HOME, config);
        String artifactRepo = validateRequiredField(ARTIFACT_REPO, config);
        String mavenRepo = validateField(MAVEN_REPO_URI, config, "");

        if (mavenRepo.isEmpty()) {
            LOG.warn("'maven.repo.uri', using maven central");
            // mavenRepo = MAVEN_CENTRAL;
        }

        DeployConfig deployconfig = new DeployConfig(vertxHome, artifactRepo, mavenRepo)
                .withConfigLocation(config)
                .withHttpPort(config)
                .withAwsConfig(config)
                .withHttpAuth(config)
                .withAuthToken(config)
                .withCluster(config)
                .withRunDir(config)
                .withLoggerFactoryName(config)
                .withRemoteRepoUpdatePolicy(config);

        if (!config.isEmpty()) {
            config.fieldNames().forEach(s -> LOG.info("Unused variable in config '{}',", s));
        }
        return deployconfig;
    }

    private DeployConfig withAuthToken(JsonObject config) {
        this.authToken = validateField(AUTH_TOKEN, config);
        return this;
    }


    private DeployConfig withConfigLocation(JsonObject config) {
        this.configLocation = config.getString(CONFIG_LOCATION, "");
        config.remove(CONFIG_LOCATION);
        return this;
    }

    private DeployConfig withRunDir(JsonObject config) {
        this.runDir = config.getString(RUN_DIR, getVertxHome()+"/run/");
        if (!runDir.endsWith("/")) {
            runDir = runDir + "/";
        }
        this.statFile = runDir + STAT_FILE;
        config.remove(RUN_DIR);
        return this;
    }

    private DeployConfig withRemoteRepoUpdatePolicy(JsonObject config) {
        this.remoteRepoPolicy = config.getString(MAVEN_SNAPSHOT_POLICY, "always");
        config.remove(MAVEN_SNAPSHOT_POLICY);
        return this;
    }

    private DeployConfig withCluster(JsonObject config) {
        this.asCluster = config.getBoolean(CLUSTER, true);
        config.remove(CLUSTER);
        return this;
    }

    private DeployConfig withLoggerFactoryName(JsonObject config) {
        this.defaultJavaOpts = config.getString(DEFAULT_JAVA_OPTS, "");
        config.remove(DEFAULT_JAVA_OPTS);
        return this;
    }

    private DeployConfig withAwsConfig(JsonObject config) {
        this.awsRegion = validateField(AWS_REGION, config, AWS_DEFAULT_REGION);
        this.awsLoadbalancerId = validateField(AWS_INSTANCE_ID, config);
        this.awsEnabled = validateField(AWS_ENABLED, config, false);

        this.awsMaxRegistrationDuration = config.getInteger(AWS_REGISTER_MAX_DURATION, 4);
        config.remove(AWS_REGISTER_MAX_DURATION);

        if (awsEnabled) {
            LOG.info("Enabled AWS support.");
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

    private DeployConfig withHttpPort(JsonObject config) {
        this.httpPort = Integer.valueOf(validateField(HTTP_PORT, config, "6789"));
        return this;

    }

    public Path getVertxHome() {
        return vertxHome;
    }

    public Path getArtifactRepo() {
        return artifactRepo;
    }

    public URI getNexusUrl() {
        return nexusUrl;
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getAwsLoadbalancerId() {
        return awsLoadbalancerId;
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

    public boolean isMavenRemote() {
        return mavenRemote;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean asCluster() {
        return asCluster;
    }

    public String getRemoteRepoPolicy() {
        return remoteRepoPolicy;
    }

    public String getDefaultJavaOpts() {
        return defaultJavaOpts;
    }

    public Integer getHttpPort() {
        return this.httpPort;
    }

    public String getRunDir() {
        return runDir;
    }

    public String getStatFile() {
        return statFile;
    }
}
