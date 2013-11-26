package nl.jpoint.vertx.mod.cluster.service;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.command.*;
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

public class DeployModuleService implements DeployService {
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

    public boolean deploy(final ModuleRequest deployRequest) {
        if (deployRequest.isSnapshot()) {
            Command resolveVersion = new ResolveSnapshotVersion(config, LogConstants.DEPLOY_REQUEST);
            JsonObject result = resolveVersion.execute(deployRequest);

            if (result.getBoolean("success")) {
                deployRequest.setSnapshotVersion(result.getString("version"));
            }
        }

        final ModuleVersion moduleInstalled = moduleInstalled(deployRequest);

        // If the module with the same version is already installed there is no need to take any further action.
        if (moduleInstalled.equals(ModuleVersion.INSTALLED)) {
            return true;
        }

        // Respond OK if the deployment is async.
        if (deployRequest.isAsync()) {
            return true;
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
            return false;
        }

        // Run the newly installed module.
        RunModule runModCommand = new RunModule();
        result = runModCommand.execute(deployRequest);

        if (!result.getBoolean(Constants.STATUS_SUCCESS) && !deployRequest.isAsync()) {
            return false;
        }

        installedModules.put(deployRequest.getMavenArtifactId(),deployRequest.getSnapshotVersion() == null ? deployRequest.getVersion() : deployRequest.getSnapshotVersion());

        LOG.info("[{} - {}]: Cleaning up after deploy", LogConstants.DEPLOY_REQUEST, deployRequest.getId());
        return true;
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
                if (deployRequest.getSnapshotVersion().equals(installedModules.get(deployRequest.getMavenArtifactId()))) {
                    LOG.info("[{} - {}]: Same SNAPSHOT version ({}) of Module {} already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest.getSnapshotVersion(), deployRequest.getModuleId());
                    return ModuleVersion.INSTALLED;
                }
                LOG.info("[{} - {}]: Older version of Module {} already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.getModuleId());
                return ModuleVersion.OLDER_VERSION;
            }
        }
        return ModuleVersion.NOT_INSTALLED;
    }
}
