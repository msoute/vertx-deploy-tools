package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class RunApplication implements Command<ModuleRequest> {

    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String INSTANCES = "INSTANCES";
    private static final Logger LOG = LoggerFactory.getLogger(RunApplication.class);
    private static final String UUID_PATTERN = "[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}";
    private boolean success = false;
    private FileSystem fs;
    private DeployConfig config;

    public RunApplication(final FileSystem fs, final DeployConfig config) {
        this.fs = fs;
        this.config = config;
    }

    public String buildRemoteRepo() {
        URI remoteRepo = config.getNexusUrl();
        if (remoteRepo != null && config.isHttpAuthentication()) {
            URIBuilder builder = new URIBuilder(remoteRepo);
            builder.setUserInfo(config.getHttpAuthUser() + ":" + config.getHttpAuthPassword());
            return builder.toString();
        }
        return config.getNexusUrl().toString();
    }

    @Override
    public JsonObject execute(final ModuleRequest request) {
        LOG.info("[{} - {}]: Running module '{}'", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());

        startWithInit(request);

        return new JsonObject()
                .put(Constants.DEPLOY_ID, request.getId().toString())
                .put(Constants.MAVEN_ID, request.getMavenArtifactId())
                .put(Constants.MODULE_VERSION, request.getSnapshotVersion() == null ? request.getVersion() : request.getSnapshotVersion())
                .put(Constants.STATUS_SUCCESS, success);
    }

    public void startWithInit(final ModuleRequest request) {
        Properties serviceProperties = readServiceDefaults(request);
        try {
            List<String> command = new ArrayList<>();
            command.addAll(Arrays.asList(config.getVertxHome().resolve("bin/vertx").toString(), "start", "maven:" + request.getModuleId(), "-id", request.getModuleId()));
            if (!config.isMavenLocal()) {
                command.add("-Dvertx.maven.remoteRepos=" + buildRemoteRepo());
                command.add("-Dvertx.maven.remoteSnapshotPolicy="+config.getRemoteRepoPolicy());
            }
            if (!config.getConfigLocation().isEmpty()) {
                command.add("-conf");
                command.add(config.getConfigLocation());
            }
            if (serviceProperties.containsKey("JAVA_OPTS")) {
                command.add("--java-opts");
                command.add(serviceProperties.getProperty(JAVA_OPTS) + " " + config.getDefaultJavaOpts());
            } else if (!config.getDefaultJavaOpts().isEmpty()) {
                command.add(config.getDefaultJavaOpts());
            }
            command.add("--instances");
            command.add((String) serviceProperties.getOrDefault(INSTANCES, "1"));

            if (config.asCluster()) {
                command.add("-cluster");
            }



           System.out.println(Arrays.toString(command.toArray()));
            final Process runProcess = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
            runProcess.waitFor(1, TimeUnit.MINUTES);

            int exitValue = runProcess.exitValue();
            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                String errorLine;
                LOG.info("[{} - {}]: {} - Error Starting module '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error(errorLine);
                }
            }

            if (exitValue == 0) {
                success = true;
                LOG.info("[{} - {}]: Started module '{}' with applicationID '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), request.getMavenArtifactId());
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to initialize module {} with error '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }
    }

    private Properties readServiceDefaults(ModuleRequest request) {
        Properties serviceProperties = new Properties();
        String path = "/etc/default/" + request.getGroupId() + ":" + request.getArtifactId();
        if (fs.existsBlocking(path)) {
            Buffer b = fs.readFileBlocking("/etc/default/" + request.getGroupId() + ":" + request.getArtifactId());
            try {
                serviceProperties.load(new ByteArrayInputStream(b.getBytes()));
            } catch (IOException e) {
                LOG.error("[{} - {}]: Failed to initialize properties for module  {} with error '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), e.getMessage());
            }
        }
        return serviceProperties;
    }
}

