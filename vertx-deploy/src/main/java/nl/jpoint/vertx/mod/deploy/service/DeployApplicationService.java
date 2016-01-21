package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.command.ResolveSnapshotVersion;
import nl.jpoint.vertx.mod.deploy.command.RunApplication;
import nl.jpoint.vertx.mod.deploy.command.StopApplication;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.ApplicationVersion;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DeployApplicationService implements DeployService<DeployApplicationRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployApplicationService.class);
    private final DeployConfig config;
    private final Map<String, JsonObject> installedModules;
    private FileSystem fs;

    public DeployApplicationService(DeployConfig config, FileSystem fs) {
        this.config = config;
        this.fs = fs;
        this.installedModules = new ProcessUtils(config).listInstalledAndRunningModules();
    }

    public JsonObject deploy(final DeployApplicationRequest deployRequest) {

        if (deployRequest.isSnapshot() && !config.isMavenLocal()) {
            ResolveSnapshotVersion resolveVersion = new ResolveSnapshotVersion(config, LogConstants.DEPLOY_REQUEST);
            JsonObject result = resolveVersion.execute(deployRequest);

            if (result.getBoolean("success")) {
                deployRequest.setSnapshotVersion(result.getString("version"));
            }
        }

        final ApplicationVersion moduleInstalled = moduleInstalled(deployRequest);

        if (moduleInstalled.equals(ApplicationVersion.ERROR)) {
            return new JsonObject().put("result", false);
        }

        // If the module with the same version is already installed there is no need to take any further action.
        if (moduleInstalled.equals(ApplicationVersion.INSTALLED)) {
            if (deployRequest.restart()) {
                RunApplication runModCommand = new RunApplication(fs, config);
                runModCommand.execute(deployRequest);
            }
            return new JsonObject().put("result", true);
        }

        if (!moduleInstalled.equals(ApplicationVersion.INSTALLED)) {
            // If an older version (or SNAPSHOT) is installed undeploy it first.
            if (moduleInstalled.equals(ApplicationVersion.OLDER_VERSION)) {
                if (!deployRequest.restart()) {
                    StopApplication stopApplicationCommand = new StopApplication(config);
                    JsonObject result = stopApplicationCommand.execute(deployRequest);
                    if (!result.getBoolean(Constants.STOP_STATUS)) {
                        return new JsonObject().put("result", false);
                    }
                }
                installedModules.remove(deployRequest.getMavenArtifactId());
            }
        }

        // Run the newly installed module.
        RunApplication runModCommand = new RunApplication(fs, config);
        JsonObject runResult = runModCommand.execute(deployRequest);

        if (!runResult.getBoolean(Constants.STATUS_SUCCESS)) {
            return new JsonObject().put("result", false);
        }

        installedModules.put(deployRequest.getMavenArtifactId(), runResult);
        return new JsonObject().put("result", true);
    }

    private ApplicationVersion moduleInstalled(ModuleRequest deployRequest) {
        if (!installedModules.containsKey(deployRequest.getMavenArtifactId())) {
            LOG.info("[{} - {}]: Module ({}) not installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest.getModuleId());
            return ApplicationVersion.NOT_INSTALLED;
        }

        JsonObject installedModule = installedModules.get(deployRequest.getMavenArtifactId());


        String requestedVersion = deployRequest.getSnapshotVersion() != null ? deployRequest.getSnapshotVersion() : deployRequest.getVersion();
        boolean sameVersion = installedModule.getString(Constants.MODULE_VERSION).equals(requestedVersion);

        if (sameVersion) {
            if (!checkModuleRunning(deployRequest)) {
                LOG.info("[{} - {}]: Module ({}) stopped externally.", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest.getModuleId());
                return ApplicationVersion.NOT_INSTALLED;
            }
            LOG.info("[{} - {}]: Module ({}) already installed.", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest.getModuleId());
        }

        return sameVersion ? ApplicationVersion.INSTALLED : ApplicationVersion.OLDER_VERSION;
    }

    private boolean checkModuleRunning(ModuleRequest deployRequest) {
        return new ProcessUtils(config).checkModuleRunning(deployRequest.getMavenArtifactId());
    }

    public void stopContainer() {
        installedModules.entrySet().stream().map(Map.Entry::getValue).forEach(module -> {
            StopApplication stopApplication = new StopApplication(config);
            String[] mavenIds = module.getString(Constants.MAVEN_ID).split(":", 2);
            stopApplication.execute(new DeployApplicationRequest(mavenIds[0], mavenIds[1], module.getString(Constants.MODULE_VERSION), true, "jar"));
        });
    }
}
