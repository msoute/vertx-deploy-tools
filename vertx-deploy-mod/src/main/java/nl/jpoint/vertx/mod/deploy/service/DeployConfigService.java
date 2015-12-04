package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.command.Command;
import nl.jpoint.vertx.mod.deploy.command.DownloadArtifact;
import nl.jpoint.vertx.mod.deploy.command.ExtractArtifact;
import nl.jpoint.vertx.mod.deploy.command.ResolveSnapshotVersion;
import nl.jpoint.vertx.mod.deploy.command.RunConsoleCommand;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.ArtifactContextUtil;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class DeployConfigService implements DeployService<DeployConfigRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployArtifactService.class);

    private final Vertx vertx;
    private final DeployConfig config;

    public DeployConfigService(Vertx vertx, DeployConfig config) {
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
            return deployResult.put("result", false);
        }
        ArtifactContextUtil artifactContextUtil = new ArtifactContextUtil(config.getArtifactRepo() + deployRequest.getFileName());
        ExtractArtifact extractConfig = new ExtractArtifact(vertx, config, Paths.get(artifactContextUtil.getBaseLocation()), false, artifactContextUtil.getCheckConfig(), LogConstants.DEPLOY_CONFIG_REQUEST);
        JsonObject extractResult = extractConfig.execute(deployRequest);

        if (artifactContextUtil.getTestCommand() != null && !artifactContextUtil.getTestCommand().isEmpty()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(deployRequest.getId().toString());
            JsonObject testResult = consoleCommand.execute(artifactContextUtil.getTestCommand());
            if (!testResult.getBoolean(Constants.COMMAND_STATUS)) {
                LOG.info("ERROR");
                return deployResult.put("result", false);
            }
        }

        if (artifactContextUtil.getRestartCommand() != null && !artifactContextUtil.getRestartCommand().isEmpty()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(deployRequest.getId().toString());
            JsonObject restartResult = consoleCommand.execute(artifactContextUtil.getRestartCommand());
            if (!restartResult.getBoolean(Constants.COMMAND_STATUS)) {
                return deployResult.put("result", false);
            }
        }
        deployResult.put("configChanged", extractResult.getBoolean("configChanged", false));

        return deployResult.put("result", extractResult.getBoolean("success"));
    }
}
