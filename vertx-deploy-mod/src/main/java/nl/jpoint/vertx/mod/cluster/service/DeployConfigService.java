package nl.jpoint.vertx.mod.cluster.service;

import nl.jpoint.vertx.mod.cluster.command.Command;
import nl.jpoint.vertx.mod.cluster.command.DownloadArtifact;
import nl.jpoint.vertx.mod.cluster.command.ExtractArtifact;
import nl.jpoint.vertx.mod.cluster.command.ResolveSnapshotVersion;
import nl.jpoint.vertx.mod.cluster.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class DeployConfigService implements DeployService<DeployConfigRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployArtifactService.class);

    private final Vertx vertx;
    private final JsonObject config;

    public DeployConfigService(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public boolean deploy(DeployConfigRequest deployRequest) {

        if (deployRequest.isSnapshot()) {
            Command<ModuleRequest> resolveVersion = new ResolveSnapshotVersion(config, LogConstants.DEPLOY_SITE_REQUEST);
            JsonObject result = resolveVersion.execute(deployRequest);

            if (result.getBoolean("success")) {
                deployRequest.setSnapshotVersion(result.getString("version"));
            }
        }

        DownloadArtifact command = new DownloadArtifact(config);
        JsonObject downloadResult = command.execute(deployRequest);

        if (!downloadResult.getBoolean("success")) {
            return false;
        }
        ExtractArtifact extractConfig = new ExtractArtifact(vertx, config, false);
        JsonObject extractResult = extractConfig.execute(deployRequest);

        return extractResult.getBoolean("success");
    }
}
