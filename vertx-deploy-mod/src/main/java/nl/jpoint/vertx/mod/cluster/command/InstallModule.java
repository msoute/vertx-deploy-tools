package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

public class InstallModule implements Command<ModuleRequest> {


    private static final Logger LOG = LoggerFactory.getLogger(InstallModule.class);

    @Override
    public JsonObject execute(final ModuleRequest request) {

        LOG.info("[{} - {}]: Installing module {}.", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());
        boolean success = false;

        try {
            final Process runProcess = Runtime.getRuntime().exec(new String[]{"/etc/init.d/vertx", "install", request.getModuleId() });
            runProcess.waitFor();

            int exitValue = runProcess.exitValue();
            if (exitValue == 0) {
                success = true;
            }

            BufferedReader output = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            String outputLine;
            while ((outputLine = output.readLine()) != null) {
                if (!outputLine.contains("Downloading") || outputLine.contains("Downloading 100%")) {
                    LOG.info("[{} - {}]: Install Module {}", LogConstants.DEPLOY_REQUEST, request.getId(), outputLine);
                }
            }

            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                String errorLine;
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error("[{} - {}]: Install module failed {}", LogConstants.DEPLOY_REQUEST, request.getId(), errorLine);
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to install module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }

        return new JsonObject()
                .putString(Constants.DEPLOY_ID, request.getId().toString())
                .putBoolean(Constants.STATUS_SUCCESS, success);
    }
}
