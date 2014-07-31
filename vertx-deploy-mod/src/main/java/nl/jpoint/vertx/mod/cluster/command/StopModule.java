package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import nl.jpoint.vertx.mod.cluster.util.ModuleFileNameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class StopModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(StopModule.class);
    private final Vertx vertx;
    private final File modRoot;

    public StopModule(Vertx vertx, File modRoot) {
        this.vertx = vertx;
        this.modRoot = modRoot;
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        Process killProcess;
        final JsonObject result = new JsonObject();
        result.putBoolean(Constants.STOP_STATUS, false);

        for (String file : modRoot.list(new ModuleFileNameFilter(request))) {
            LOG.info("[{} - {}]: Stopping module {}", LogConstants.DEPLOY_REQUEST, request.getId(), file);

            try {
                killProcess = Runtime.getRuntime().exec(new String[]{"sudo", "/etc/init.d/vertx", "stop-module", file});
                killProcess.waitFor();
                int exitValue = killProcess.exitValue();
                BufferedReader output = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));
                String outputLine;
                while ((outputLine = output.readLine()) != null) {
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
                return result;
            }
        }
        return result;
    }

}
