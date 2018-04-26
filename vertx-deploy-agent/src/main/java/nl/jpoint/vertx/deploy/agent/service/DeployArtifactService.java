package nl.jpoint.vertx.deploy.agent.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.request.DeployArtifactRequest;
import nl.jpoint.vertx.deploy.agent.request.ModuleRequest;
import nl.jpoint.vertx.deploy.agent.util.GzipExtractor;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
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
                                .flatMap(this::deflateGzip)
                                .flatMap(this::parseArtifactContext)
                                .flatMap(this::extractArtifact)
                                .flatMap(this::addInstalledVersion);
                    }
                })
                .doOnCompleted(() -> LOG.info("[{} - {}]: Done extracting artifact {}.", deployRequest.getLogName(), deployRequest.getId(), deployRequest.getModuleId()));
    }

    private Observable<DeployArtifactRequest> deflateGzip(DeployArtifactRequest deployArtifactRequest) {
        if (ModuleRequest.GZIP_TYPE.equals(deployArtifactRequest.getType())) {
            new GzipExtractor<>(deployArtifactRequest).deflateGz(deployArtifactRequest.getLocalPath(config.getArtifactRepo()));
        }
        return Observable.just(deployArtifactRequest);
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
