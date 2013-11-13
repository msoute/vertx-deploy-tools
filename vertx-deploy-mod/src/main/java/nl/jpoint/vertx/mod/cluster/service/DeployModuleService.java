package nl.jpoint.vertx.mod.cluster.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.command.InstallModule;
import nl.jpoint.vertx.mod.cluster.command.RunModule;
import nl.jpoint.vertx.mod.cluster.command.UndeployModule;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import nl.jpoint.vertx.mod.cluster.util.ModuleFileNameFilter;
import nl.jpoint.vertx.mod.cluster.util.ModuleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import java.io.File;

public class DeployModuleService implements DeployService {
    private static final Logger LOG = LoggerFactory.getLogger(DeployModuleService.class);
    private static final String MODS_DIR_PROP_NAME = "vertx.mods";
    private static final String LOCAL_MODS_DIR = "mods";
    private final Vertx vertx;
    private final PlatformManager platformManager;
    private final File modRoot;

    public DeployModuleService(final Vertx vertx) {
        this.vertx = vertx;
        platformManager = PlatformLocator.factory.createPlatformManager();

        String modDir = System.getProperty(MODS_DIR_PROP_NAME);
        if (modDir != null && !modDir.trim().equals("")) {
            modRoot = new File(modDir);
        } else {
            // Default to local module directory
            modRoot = new File(LOCAL_MODS_DIR);
        }

    }

    public void deploy(final ModuleRequest deployRequest, final HttpServerRequest request) {

        final ModuleVersion moduleInstalled = moduleInstalled(deployRequest);

        // If the module with the same version is already installed there is no need to take any further action.
        if (moduleInstalled.equals(ModuleVersion.INSTALLED)) {
            respondOk(request);
            return;
        }

        // Respond OK if the deployment is async.
        if (deployRequest.isAsync()) {
            respondOk(request);
        }

        // If an older version (or SNAPSHOT) is installed undeploy it first.
        if (moduleInstalled.equals(ModuleVersion.OLDER_VERSION)) {
            UndeployModule undeployCommand = new UndeployModule(vertx, modRoot);
            undeployCommand.execute(deployRequest);
        }

        // Install the new module.
        InstallModule installCommand = new InstallModule(platformManager, vertx.eventBus());
        JsonObject result = installCommand.execute(deployRequest);

        // Respond failed if install did not complete.
        if (!result.getBoolean(Constants.STATUS_SUCCESS)) {
            this.respondFailed(request);
            return;
        }

        // Run the newly installed module.
        RunModule runModCommand = new RunModule();
        result = runModCommand.execute(deployRequest);

        if (!result.getBoolean(Constants.STATUS_SUCCESS) && !deployRequest.isAsync()) {
            this.respondFailed(request);
            return;
        }

        LOG.info("[{} - {}] : Cleaning up after deploy", LogConstants.DEPLOY_REQUEST, deployRequest.getId());

        if (!deployRequest.isAsync()) {
            respondOk(request);
        }

    }

    private void respondOk(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        request.response().end();
    }

    private void respondFailed(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        request.response().end();
    }

    private ModuleVersion moduleInstalled(ModuleRequest deployRequest) {

        if (!modRoot.exists()) {
            return ModuleVersion.NOT_INSTALLED;
        }

        for (String mod : modRoot.list(new ModuleFileNameFilter(deployRequest))) {
            if (mod.equals(deployRequest.getModuleId()) && !deployRequest.isSnapshot()) {
                LOG.info("[{} - {}]: Module {} already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.getModuleId());
                return ModuleVersion.INSTALLED;
            } else {
                LOG.info("[{} - {}]: Older version of Module {} already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.getModuleId());
                return ModuleVersion.OLDER_VERSION;
            }
        }
        return ModuleVersion.NOT_INSTALLED;
    }
}
