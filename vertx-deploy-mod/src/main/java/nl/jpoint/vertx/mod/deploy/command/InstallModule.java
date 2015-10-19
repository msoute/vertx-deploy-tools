package nl.jpoint.vertx.mod.deploy.command;

import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstallModule implements Command<ModuleRequest> {


    private static final Logger LOG = LoggerFactory.getLogger(InstallModule.class);
    private final PlatformManager platformManager;
    private boolean success = false;
    private AtomicBoolean isDone = new AtomicBoolean(false);

    private final boolean deployInternal;

    public InstallModule(final PlatformManager platformManager, final JsonObject config) {
        this.platformManager = platformManager;
        this.deployInternal = config.getBoolean("deploy.internal", false);
    }

    @Override
    public JsonObject execute(final ModuleRequest request) {

        LOG.info("[{} - {}]: Installing module '{}'", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());

        if (deployInternal) {
            deployWithManager(request);
        } else {
            deployWithInit(request);
        }

        return new JsonObject()
                .putString(Constants.DEPLOY_ID, request.getId().toString())
                .putBoolean(Constants.STATUS_SUCCESS, success);
    }


    private void deployWithManager(final ModuleRequest request) {
        platformManager.installModule(request.getModuleId(), voidAsyncResult -> {
            success = voidAsyncResult.succeeded();
            isDone.set(true);
            LOG.info("[{} - {}]: Install Module succeeded {}", LogConstants.DEPLOY_REQUEST, request.getId(), success);
        });

        while (!isDone.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.error("[{} - {}]: Error while Installing module {}, 'e:{}' ", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId(), e.getMessage());
                success = false;
            }
        }
    }

    private void deployWithInit(final ModuleRequest request) {
        try {
            final Process runProcess = Runtime.getRuntime().exec(new String[]{"/etc/init.d/vertx", "install", request.getModuleId()});
            runProcess.waitFor();

            int exitValue = runProcess.exitValue();
            if (exitValue == 0) {
                success = true;
            }

            BufferedReader output = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            String outputLine;
            while ((outputLine = output.readLine()) != null) {
                if (!outputLine.contains("Downloading") && !outputLine.isEmpty()) {
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
    }
}
