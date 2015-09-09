package nl.jpoint.vertx.mod.deploy.command;

import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * TODO : Vertx homedir should be configurable.
 */
public class RunModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(RunModule.class);
    private boolean success = false;

    private final boolean deployInternal;
    private final String vertxHome;

    public RunModule(final PlatformManager platformManager, final JsonObject config) {
        this.deployInternal = config.getBoolean("deploy.internal", false);
        this.vertxHome = config.getString("vertx.home");
    }

    @Override
    public JsonObject execute(final ModuleRequest request) {
        LOG.info("[{} - {}]: Running module {}.", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());

        if (deployInternal) {
            startFromContainer(request);
        } else {
            startWithInit(request);
        }

        return new JsonObject()
                .putString(Constants.DEPLOY_ID, request.getId().toString())
                .putBoolean(Constants.STATUS_SUCCESS, success);
    }

    private void startFromContainer(final ModuleRequest request) {
        try {
            System.setProperty("jdk.lang.Process.launchMechanism", "fork");
            Process p = Runtime.getRuntime().exec(new String[]{vertxHome + "/bin/vertx", "runmod", request.getModuleId(), "-instances", String.valueOf(((DeployModuleRequest) request).getInstances()), "-conf", vertxHome + "/mods/" + request.getModuleId() + "/config.json"}, null, new File(vertxHome));
            p.getErrorStream().close();
            p.getOutputStream().close();
            ProcessUtils.writePid(request.getModuleId());
            success = true;
        } catch (IOException e) {
            LOG.error("[{} - {}]: Failed to initialize module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }
    }

    public void startWithInit(final ModuleRequest request) {
        try {
            final Process runProcess = Runtime.getRuntime().exec(new String[]{"/etc/init.d/vertx", "start-module", request.getModuleId(), String.valueOf(((DeployModuleRequest) request).getInstances())});

            runProcess.waitFor(1, TimeUnit.MINUTES);

            int exitValue = runProcess.exitValue();
            if (exitValue == 0) {
                success = true;
                LOG.info("[{} - {}]: Started module '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
            }

            if (exitValue != 0) {
                LOG.info("[{} - {}]: {} - Error Starting module '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to initialize module {} with error '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }
    }
}

