package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * TODO : Vertx homedir should be configurable.
 */
public class RunModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(RunModule.class);


    @Override
    public JsonObject execute(final ModuleRequest request) {
        LOG.info("[{} - {}]: Running module {}.", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());
        boolean success = false;

        try {
            final Process runProcess = Runtime.getRuntime().exec(new String[]{"/etc/init.d/vertx", "start-module", request.getModuleId(), String.valueOf(((DeployModuleRequest) request).getInstances())});
            runProcess.waitFor(1, TimeUnit.MINUTES);

            int exitValue = runProcess.exitValue();
            if (exitValue == 0) {
                success = true;
                LOG.info("[{} - {}]: {} - Started module '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
            }

            if (exitValue != 0) {
                LOG.info("[{} - {}]: {} - Error Starting module '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to initialize module {} with error '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }

        return new JsonObject()
                .putString(Constants.DEPLOY_ID, request.getId().toString())
                .putBoolean(Constants.STATUS_SUCCESS, success);
    }
}

