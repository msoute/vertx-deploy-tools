package nl.jpoint.vertx.mod.cluster.service;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.command.Command;
import nl.jpoint.vertx.mod.cluster.command.InstallModule;
import nl.jpoint.vertx.mod.cluster.command.InvokeContainer;
import nl.jpoint.vertx.mod.cluster.command.ResolveSnapshotVersion;
import nl.jpoint.vertx.mod.cluster.command.RunModule;
import nl.jpoint.vertx.mod.cluster.command.StopModule;
import nl.jpoint.vertx.mod.cluster.command.UndeployModule;
import nl.jpoint.vertx.mod.cluster.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import nl.jpoint.vertx.mod.cluster.util.ModuleFileNameFilter;
import nl.jpoint.vertx.mod.cluster.util.ModuleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import java.io.File;

public class DeployModuleService implements DeployService<DeployModuleRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployModuleService.class);
    private final Vertx vertx;
    private final JsonObject config;
    private final PlatformManager platformManager;
    private final File modRoot;
    private final ConcurrentSharedMap<String, String> installedModules;

    public DeployModuleService(final Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        this.platformManager = PlatformLocator.factory.createPlatformManager();

        this.modRoot = new File(config.getString("mod.root"));
        this.installedModules = this.vertx.sharedData().getMap("installed_modules");
    }

    public boolean deploy(final DeployModuleRequest deployRequest) {

        if (deployRequest.isSnapshot()) {
            Command resolveVersion = new ResolveSnapshotVersion(config, LogConstants.DEPLOY_REQUEST);
            JsonObject result = resolveVersion.execute(deployRequest);

            if (result.getBoolean("success")) {
                deployRequest.setSnapshotVersion(result.getString("version"));
            }
        }

        final ModuleVersion moduleInstalled = moduleInstalled(deployRequest);

        if (moduleInstalled.equals(ModuleVersion.ERROR)) {
            return false;
        }

        // If the module with the same version is already installed there is no need to take any further action.
        if (moduleInstalled.equals(ModuleVersion.INSTALLED)) {
            if (deployRequest.restart()) {
                RunModule runModCommand = new RunModule();
                runModCommand.execute(deployRequest);
            }
            return true;
        }

        if (!moduleInstalled.equals(ModuleVersion.INSTALLED)) {
            // Respond OK if the deployment is async.
            if (deployRequest.isAsync()) {
                return true;
            }

            // If an older version (or SNAPSHOT) is installed undeploy it first.
            if (moduleInstalled.equals(ModuleVersion.OLDER_VERSION)) {
                if (!deployRequest.restart()) {
                    StopModule stopModuleCommand = new StopModule(vertx, modRoot);
                    JsonObject result = stopModuleCommand.execute(deployRequest);

                    if (!result.getBoolean(Constants.STOP_STATUS)) {
                        return false;
                    }
                }

                UndeployModule undeployCommand = new UndeployModule(vertx, modRoot);
                undeployCommand.execute(deployRequest);

                //after undeploying remove previous installed version from cache
                installedModules.remove(deployRequest.getModuleIdentifier());

            }

            // Install the new module.
            InstallModule installCommand = new InstallModule();
            JsonObject installResult = installCommand.execute(deployRequest);

            // Respond failed if install did not complete.
            if (!installResult.getBoolean(Constants.STATUS_SUCCESS)) {
                return false;
            }
        }

        // Run the newly installed module.
        RunModule runModCommand = new RunModule();
        JsonObject runResult = runModCommand.execute(deployRequest);

        if (!runResult.getBoolean(Constants.STATUS_SUCCESS) && !deployRequest.isAsync()) {
            return false;
        }

        installedModules.put(deployRequest.getModuleIdentifier(), deployRequest.getSnapshotVersion() == null ? deployRequest.getVersion() : deployRequest.getSnapshotVersion());

        LOG.info("[{} - {}]: Cleaning up after deploy", LogConstants.DEPLOY_REQUEST, deployRequest.getId());
        return true;
    }

    private ModuleVersion moduleInstalled(ModuleRequest deployRequest) {

        if (!modRoot.exists()) {
            LOG.error("[{} - {}]: Module root {} Does not exist, trying to create.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), modRoot);
            boolean result = modRoot.mkdirs();
            if (!result) {
                LOG.error("[{} - {}]: Failed to create module root {}.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), modRoot);
                return ModuleVersion.ERROR;
            } else {
                LOG.info("[{} - {}]: Created module root {}.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), modRoot);
            }

        }

        for (String mod : modRoot.list(new ModuleFileNameFilter(deployRequest))) {
            if (mod.equals(deployRequest.getModuleId()) && !deployRequest.isSnapshot()) {
                LOG.info("[{} - {}]: Module {} already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.getModuleIdentifier());
                return ModuleVersion.INSTALLED;
            } else {
                if (deployRequest.isSnapshot() && deployRequest.getSnapshotVersion().equals(installedModules.get(deployRequest.getModuleIdentifier()))) {
                    LOG.info("[{} - {}]: Same SNAPSHOT version ({}) of Module {} already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest.getSnapshotVersion(), deployRequest.getModuleId());
                    return ModuleVersion.INSTALLED;
                }
                LOG.info("[{} - {}]: Older version of Module {} already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.getModuleId());
                return ModuleVersion.OLDER_VERSION;
            }
        }
        return ModuleVersion.NOT_INSTALLED;
    }

    public void stopContainer(String deployId) {
        Command<String> stopContainer = new InvokeContainer(deployId);
        stopContainer.execute("stop");
    }
}
