package nl.jpoint.vertx.mod.deploy.service;

import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.command.*;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.ArtifactContextUtil;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.nio.file.Paths;

public class DeployConfigService implements DeployService<DeployConfigRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployArtifactService.class);

    private final Vertx vertx;
    private final JsonObject config;

    public DeployConfigService(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public JsonObject deploy(DeployConfigRequest deployRequest) {
        JsonObject deployResult = new JsonObject();
        if (deployRequest.isSnapshot()) {
            Command<ModuleRequest> resolveVersion = new ResolveSnapshotVersion(config, LogConstants.DEPLOY_CONFIG_REQUEST);
            JsonObject result = resolveVersion.execute(deployRequest);

            if (result.getBoolean("success")) {
                deployRequest.setSnapshotVersion(result.getString("version"));
            }
        }

        DownloadArtifact downloadArtifact = new DownloadArtifact(config);
        JsonObject downloadResult = downloadArtifact.execute(deployRequest);

        if (!downloadResult.getBoolean("success")) {
            return deployResult.putBoolean("result", false);
        }
        ArtifactContextUtil artifactContextUtil = new ArtifactContextUtil(config.getString("artifact.repo") + "/" + deployRequest.getFileName());
        if (artifactContextUtil.getBaseLocation() == null || artifactContextUtil.getBaseLocation().isEmpty()) {
            LOG.info("ERROR");
            return deployResult.putBoolean("result", false);
        }
        ExtractArtifact extractConfig = new ExtractArtifact(vertx, config, Paths.get(artifactContextUtil.getBaseLocation()), false, artifactContextUtil.getCheckConfig(), LogConstants.DEPLOY_CONFIG_REQUEST);
        JsonObject extractResult = extractConfig.execute(deployRequest);

        if (artifactContextUtil.getTestCommand() != null && !artifactContextUtil.getTestCommand().isEmpty()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(deployRequest.getId().toString());
            JsonObject testResult = consoleCommand.execute(artifactContextUtil.getTestCommand());
            if (!testResult.getBoolean(Constants.COMMAND_STATUS)) {
                LOG.info("ERROR");
                return deployResult.putBoolean("result", false);
            }
        }

        if (artifactContextUtil.getRestartCommand() != null && !artifactContextUtil.getRestartCommand().isEmpty()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(deployRequest.getId().toString());
            JsonObject restartResult = consoleCommand.execute(artifactContextUtil.getRestartCommand());
            if (!restartResult.getBoolean(Constants.COMMAND_STATUS)) {
                return deployResult.putBoolean("result", false);
            }
        }
        deployResult.putBoolean("configChanged", extractResult.getBoolean("configChanged", false));

        return deployResult.putBoolean("result", extractResult.getBoolean("success"));
    }
}
