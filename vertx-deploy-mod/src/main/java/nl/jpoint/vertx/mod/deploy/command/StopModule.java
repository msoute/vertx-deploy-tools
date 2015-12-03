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

public class StopModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(StopModule.class);
    final JsonObject result = new JsonObject();
    private final String vertxHome;
    private String applicationId;

    public StopModule(JsonObject config) {
        this.vertxHome = config.getString("vertx.home");
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        result.putBoolean(Constants.STOP_STATUS, false);
        stopWithInit(request, applicationId);

        return result;
    }

    public void stopWithInit(ModuleRequest request, String applicationId) {
        LOG.info("[{} - {}]: Stopping module '{}' with applicationId '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), applicationId);
        Process killProcess;
        try {
            killProcess = Runtime.getRuntime().exec(new String[]{vertxHome + "/bin/vertx", "stop", applicationId});
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
            result.putBoolean(Constants.STOP_STATUS, true);
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to stop module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }
    }

    public StopModule forApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

}
