package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StopApplication implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(StopApplication.class);
    final JsonObject result = new JsonObject();
    private DeployConfig config;
    private ProcessUtils processUtils;

    public StopApplication(DeployConfig config) {
        this.config = config;
        this.processUtils = new ProcessUtils(config);
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        result.put(Constants.STOP_STATUS, false);
        stopWithInit(request);
        LOG.info("[{} - {}]: Waiting for module {} to stop.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            while (processUtils.checkModuleRunning(request.getMavenArtifactId())) {
                // Wait for stop
            }
            return "42";
        });
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.info("[{} - {}]: Error while Waiting for  module '{}' with applicationId '{}' to stop -> '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), e);
        }
        LOG.info("[{} - {}]: Module '{}' stopped.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
        return result;
    }

    public void stopWithInit(ModuleRequest request) {
        LOG.info("[{} - {}]: Stopping module '{}' with applicationId '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        Process killProcess;
        try {
            killProcess = Runtime.getRuntime().exec(new String[]{config.getVertxHome().resolve("bin/vertx").toString(), "stop", request.getMavenArtifactId()});
            killProcess.waitFor(1, TimeUnit.MINUTES);
            int exitValue = killProcess.exitValue();
            BufferedReader output = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));
            String outputLine;
            while ((outputLine = output.readLine()) != null && !outputLine.isEmpty()) {
                LOG.info("[{} - {}]: {}", LogConstants.DEPLOY_REQUEST, request.getId(), outputLine);
            }

            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(killProcess.getErrorStream()));
                String errorLine;
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error("[{} - {}]: {}", LogConstants.DEPLOY_REQUEST, request.getId(), errorLine);
                }
            }
            result.put(Constants.STOP_STATUS, true);
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to stop module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }
    }
}
