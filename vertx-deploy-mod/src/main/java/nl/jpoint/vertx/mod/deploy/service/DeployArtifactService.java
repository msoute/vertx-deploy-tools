package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.command.Command;
import nl.jpoint.vertx.mod.deploy.command.DownloadArtifact;
import nl.jpoint.vertx.mod.deploy.command.ExtractArtifact;
import nl.jpoint.vertx.mod.deploy.command.ResolveSnapshotVersion;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.util.ArtifactContextUtil;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DeployArtifactService implements DeployService<DeployArtifactRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployArtifactService.class);

    private final Vertx vertx;
    private final DeployConfig config;
    private final Map<String, String> installedArtifacts;

    public DeployArtifactService(Vertx vertx, DeployConfig config) {
        this.vertx = vertx;
        this.config = config;
        this.installedArtifacts = new HashMap<>();
    }

    @Override
    public JsonObject deploy(DeployArtifactRequest deployRequest) {

        if (deployRequest.isSnapshot()) {
            Command resolveVersion = new ResolveSnapshotVersion(config, LogConstants.DEPLOY_SITE_REQUEST);
            JsonObject result = resolveVersion.execute(deployRequest);

            if (result.getBoolean("success")) {
                deployRequest.setSnapshotVersion(result.getString("version"));
            }
        }

        if (installedArtifacts.containsKey(deployRequest.getGroupId() + ":" + deployRequest.getArtifactId())
                && installedArtifacts.get(deployRequest.getGroupId() + ":" + deployRequest.getArtifactId()).equals(deployRequest.getSnapshotVersion())) {
            LOG.info("[{} - {}]: Same SNAPSHOT version ({}) of Artifact {} already installed.", LogConstants.DEPLOY_SITE_REQUEST, deployRequest.getId(), deployRequest.getSnapshotVersion(), deployRequest.getModuleId());
            new JsonObject().put("result", true);
        }

        DownloadArtifact downloadArtifactCommand = new DownloadArtifact(config);
        JsonObject downloadResult = downloadArtifactCommand.execute(deployRequest);

        if (!downloadResult.getBoolean("success")) {
            new JsonObject().put("result", false);
        }
        ArtifactContextUtil artifactContextUtil = new ArtifactContextUtil(config.getArtifactRepo() + deployRequest.getFileName());

        ExtractArtifact extractSite = new ExtractArtifact(vertx, config, Paths.get(artifactContextUtil.getBaseLocation()), true, false, LogConstants.DEPLOY_SITE_REQUEST);
        JsonObject extractResult = extractSite.execute(deployRequest);

        if (deployRequest.getSnapshotVersion() != null) {
            installedArtifacts.put(deployRequest.getGroupId() + ":" + deployRequest.getArtifactId(), deployRequest.getSnapshotVersion());
        }
        return new JsonObject().put("result", extractResult.getBoolean("success"));
    }
}
