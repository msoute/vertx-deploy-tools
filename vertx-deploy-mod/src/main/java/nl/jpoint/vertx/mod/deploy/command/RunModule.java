package nl.jpoint.vertx.mod.deploy.command;

import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class RunModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(RunModule.class);
    private static final String UUID_PATTERN = "[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}";
    private final String vertxHome;
    private final String mavenRepoUrl;
    private final String mavenUser;
    private final String mavenPassword;
    private final String protocol;
    private final String configFile;
    private boolean success = false;

    public RunModule(final JsonObject config) {
        this.vertxHome = config.getString("vertx.home");
        this.mavenRepoUrl = config.getString("http.authUri", null);
        this.mavenUser = config.getString("http.authUser", null);
        this.mavenPassword = config.getString("http.authPass", null);
        this.protocol = config.getBoolean("http.authSecure", false) ? "https://" : "http://";
        this.configFile = config.getString("config.location");
    }

    public String buildRemoteRepo() {
        if (mavenRepoUrl != null) {
            StringBuilder builder = new StringBuilder(protocol);
            if (mavenUser != null) {
                builder.append(mavenUser).append(":").append(mavenPassword).append("@");
            }
            builder.append(mavenRepoUrl);
            return builder.toString();
        }
        return null;
    }

    @Override
    public JsonObject execute(final ModuleRequest request) {
        LOG.info("[{} - {}]: Running module '{}'", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());

        String applicationID = startWithInit(request);

        return new JsonObject()
                .putString(Constants.DEPLOY_ID, request.getId().toString())
                .putString(Constants.MAVEN_ID, request.getMavenArtifactId())
                .putString(Constants.MODULE_VERSION, request.getSnapshotVersion() == null ? request.getVersion() : request.getSnapshotVersion())
                .putString(Constants.APPLICATION_ID, applicationID)
                .putBoolean(Constants.STATUS_SUCCESS, success);
    }

    public String startWithInit(final ModuleRequest request) {
        String applicationId = "";
        try {
            final Process runProcess = Runtime.getRuntime().exec(new String[]{vertxHome + "/bin/vertx", "start", "maven:" + request.getModuleId(), "-conf=" + configFile, "-Dvertx.maven.remoteRepos=" + buildRemoteRepo()});
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

                BufferedReader out = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String outLine;
                while ((outLine = out.readLine()) != null) {
                    if (outLine.matches(UUID_PATTERN)) {
                        applicationId = outLine;
                    }
                }
                LOG.info("[{} - {}]: Started module '{}' with applicationID '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), applicationId);
            }

        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to initialize module {} with error '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }

        return applicationId;
    }
}

