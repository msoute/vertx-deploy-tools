package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import nl.jpoint.vertx.mod.cluster.util.ModuleFileNameFilter;
import nl.jpoint.vertx.mod.cluster.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class StopModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(StopModule.class);

    private boolean success = false;
    private AtomicBoolean isDone = new AtomicBoolean(false);

    private final boolean stopWithInit;


    private final Vertx vertx;
    private final File modRoot;
    private final boolean olderVersion;
    final JsonObject result = new JsonObject();

    public StopModule(Vertx vertx, File modRoot, JsonObject config, boolean olderVersion) {
        this.vertx = vertx;
        this.modRoot = modRoot;
        this.olderVersion = olderVersion;
        this.stopWithInit = (config.containsField("deploy.internal") && !config.getBoolean("deploy.internal"));
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        result.putBoolean(Constants.STOP_STATUS, false);
        if (stopWithInit) {
            stopWithInit(request);
        } else {
            stopWithManager(request);
        }
        return result;
    }

    private void stopWithManager(ModuleRequest request) {
        for (String file : modRoot.list(new ModuleFileNameFilter(request))) {
            String moduleId = request.getModuleId();
            if (olderVersion) {
                moduleId = request.getModuleId().substring(0, request.getModuleId().lastIndexOf('~'));
            }
            List<Integer> pids = ProcessUtils.findPidsForModule(moduleId, LogConstants.DEPLOY_REQUEST, request.getId().toString());
            ProcessUtils.stopProcesses(pids, LogConstants.DEPLOY_REQUEST, request.getModuleId(), request.getId());
            result.putBoolean(Constants.STOP_STATUS, true);
        }

    }

    public void stopWithInit(ModuleRequest request) {

        for (String file : modRoot.list(new ModuleFileNameFilter(request))) {
            LOG.info("[{} - {}]: Stopping module {}", LogConstants.DEPLOY_REQUEST, request.getId(), file);
            Process killProcess;
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
            }
        }

    }

}
