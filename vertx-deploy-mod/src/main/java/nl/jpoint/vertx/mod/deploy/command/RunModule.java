package nl.jpoint.vertx.mod.deploy.command;

import com.sun.javafx.fxml.builder.URLBuilder;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class RunModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(RunModule.class);
    private static final String UUID_PATTERN = "[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}";

    private boolean success = false;
    private DeployConfig config;

    public RunModule(final DeployConfig config) {
        this.config = config;
    }

    public String buildRemoteRepo() {
        URI remoteRepo = config.getNexusUrl();
        if (remoteRepo != null && config.isHttpAuthentication()) {
            URIBuilder builder = new URIBuilder(remoteRepo);
            builder.setUserInfo(config.getHttpAuthUser()+":"+config.getHttpAuthPassword());
            return builder.toString();
        }
        return null;
    }

    @Override
    public JsonObject execute(final ModuleRequest request) {
        LOG.info("[{} - {}]: Running module '{}'", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());

        String applicationID = startWithInit(request);

        return new JsonObject()
                .put(Constants.DEPLOY_ID, request.getId().toString())
                .put(Constants.MAVEN_ID, request.getMavenArtifactId())
                .put(Constants.MODULE_VERSION, request.getSnapshotVersion() == null ? request.getVersion() : request.getSnapshotVersion())
                .put(Constants.APPLICATION_ID, applicationID)
                .put(Constants.STATUS_SUCCESS, success);
    }

    public String startWithInit(final ModuleRequest request) {
        String applicationId = "";
        try {
            final Process runProcess = Runtime.getRuntime().exec(new String[]{config.getVertxHome() + "bin/vertx", "start", "maven:" + request.getModuleId(), "-conf=" + config.getConfigLocation(), "-Dvertx.maven.remoteRepos=" + buildRemoteRepo()});
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

