package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;

public class DeployArtifactService implements DeployService<DeployArtifactRequest, DeployArtifactRequest> {
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
    public Observable<DeployArtifactRequest> deployAsync(DeployArtifactRequest deployRequest) {
        return resolveSnapShotVersion(deployRequest)
                .flatMap(r -> {
                    if (versionInstalled(deployRequest)) {
                        return Observable.just(r);
                    } else {
                        return this.downloadArtifact(r)
                                .flatMap(this::parseArtifactContext)
                                .flatMap(this::extractArtifact)
                                .flatMap(this::addInstalledVersion);
                    }
                });
    }

    private boolean versionInstalled(DeployArtifactRequest deployRequest) {
        if (installedArtifacts.containsKey(deployRequest.getGroupId() + ":" + deployRequest.getArtifactId())
                && installedArtifacts.get(deployRequest.getGroupId() + ":" + deployRequest.getArtifactId()).equals(deployRequest.getVersion())) {
            LOG.info("[{} - {}]: Same SNAPSHOT version ({}) of Artifact {} already installed.", LogConstants.DEPLOY_ARTIFACT_REQUEST, deployRequest.getId(), deployRequest.getVersion(), deployRequest.getModuleId());
            return true;
        }
        return false;
    }

    private Observable<DeployArtifactRequest> addInstalledVersion(DeployArtifactRequest deployArtifactRequest) {
        installedArtifacts.put(deployArtifactRequest.getGroupId() + ":" + deployArtifactRequest.getArtifactId(), deployArtifactRequest.getVersion());
        return Observable.just(deployArtifactRequest);
    }

    @Override
    public DeployConfig getConfig() {
        return config;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }
}
