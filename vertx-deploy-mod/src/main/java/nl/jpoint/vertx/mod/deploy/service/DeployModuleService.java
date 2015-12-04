package nl.jpoint.vertx.mod.deploy.service;

import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.command.InvokeContainer;
import nl.jpoint.vertx.mod.deploy.command.ResolveSnapshotVersion;
import nl.jpoint.vertx.mod.deploy.command.RunModule;
import nl.jpoint.vertx.mod.deploy.command.StopModule;
import nl.jpoint.vertx.mod.deploy.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ModuleVersion;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.util.Map;

public class DeployModuleService implements DeployService<DeployModuleRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployModuleService.class);
    private final Vertx vertx;
    private final JsonObject config;
    private final File modRoot;
    private final Map<String, JsonObject> installedModules;

    public DeployModuleService(final Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        this.modRoot = new File(config.getString("mod.root"));
        this.installedModules = new ProcessUtils(config).listInstalledAndRunningModules();
    }

    public JsonObject deploy(final DeployModuleRequest deployRequest) {

        if (deployRequest.isSnapshot()) {
            ResolveSnapshotVersion resolveVersion = new ResolveSnapshotVersion(config, LogConstants.DEPLOY_REQUEST);
            JsonObject result = resolveVersion.execute(deployRequest);

            if (result.getBoolean("success")) {
                deployRequest.setSnapshotVersion(result.getString("version"));
            }
        }

        final ModuleVersion moduleInstalled = moduleInstalled(deployRequest);

        if (moduleInstalled.equals(ModuleVersion.ERROR)) {
            return new JsonObject().putBoolean("result", false);
        }

        // If the module with the same version is already installed there is no need to take any further action.
        if (moduleInstalled.equals(ModuleVersion.INSTALLED)) {
            if (deployRequest.restart()) {
                RunModule runModCommand = new RunModule(config);
                runModCommand.execute(deployRequest);
            }
            return new JsonObject().putBoolean("result", true);
        }

        if (!moduleInstalled.equals(ModuleVersion.INSTALLED)) {
            // If an older version (or SNAPSHOT) is installed undeploy it first.
            if (moduleInstalled.equals(ModuleVersion.OLDER_VERSION)) {
                if (!deployRequest.restart()) {
                    StopModule stopModuleCommand = new StopModule(config)
                            .forApplicationId(installedModules.get(deployRequest.getMavenArtifactId())
                                    .getString(Constants.APPLICATION_ID));
                    JsonObject result = stopModuleCommand.execute(deployRequest);

                    if (!result.getBoolean(Constants.STOP_STATUS)) {
                        return new JsonObject().putBoolean("result", false);
                    }
                }
                installedModules.remove(deployRequest.getMavenArtifactId());
            }
        }

        // Run the newly installed module.
        RunModule runModCommand = new RunModule(config);
        JsonObject runResult = runModCommand.execute(deployRequest);

        if (!runResult.getBoolean(Constants.STATUS_SUCCESS)) {
            return new JsonObject().putBoolean("result", false);
        }

        installedModules.put(deployRequest.getMavenArtifactId(), runResult);
        return new JsonObject().putBoolean("result", true);
    }

    private ModuleVersion moduleInstalled(ModuleRequest deployRequest) {
        if (!installedModules.containsKey(deployRequest.getMavenArtifactId())) {
            LOG.info("[{} - {}]: Module ({}) not installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest.getModuleId());
            return ModuleVersion.NOT_INSTALLED;
        }

        JsonObject installedModule = installedModules.get(deployRequest.getMavenArtifactId());

        boolean sameVersion = installedModule.getString(Constants.MODULE_VERSION).equals(deployRequest.getSnapshotVersion() != null ? deployRequest.getSnapshotVersion() : deployRequest.getVersion());

        if (sameVersion) {
            LOG.info("[{} - {}]: Module ({}) already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest.getModuleId());
        }

        return sameVersion ? ModuleVersion.INSTALLED : ModuleVersion.OLDER_VERSION;
    }

    public void stopContainer(String requestId) {
        InvokeContainer invokeContainer = new InvokeContainer(requestId, config);
        installedModules.entrySet().stream().map(Map.Entry::getValue).forEach(module -> {
            invokeContainer.withArgs(module.getString(Constants.APPLICATION_ID));
            invokeContainer.execute("stop");
        });
    }
}
