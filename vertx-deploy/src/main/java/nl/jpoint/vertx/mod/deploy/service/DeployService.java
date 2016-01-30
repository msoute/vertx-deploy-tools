package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.command.DownloadArtifact;
import nl.jpoint.vertx.mod.deploy.command.ExtractArtifact;
import nl.jpoint.vertx.mod.deploy.command.ResolveSnapshotVersion;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.ArtifactContextUtil;
import rx.Observable;

import static rx.Observable.just;

interface DeployService<T extends ModuleRequest, R> {
    Observable<R> deployAsync(T deployRequest);

    DeployConfig getConfig();

    Vertx getVertx();

    default Observable<T> resolveSnapShotVersion(T moduleRequest) {
        if (moduleRequest.isSnapshot() && getConfig().isMavenRemote()) {
            ResolveSnapshotVersion<T> resolveVersion = new ResolveSnapshotVersion<>(getConfig(), getVertx());
            return resolveVersion.executeAsync(moduleRequest);
        } else {
            return just(moduleRequest);
        }
    }

    default Observable<T> downloadArtifact(T moduleRequest) {
        DownloadArtifact<T> downloadArtifact = new DownloadArtifact<>(getConfig(), getVertx());
        return downloadArtifact.executeAsync(moduleRequest);
    }

    default Observable<T> parseArtifactContext(T moduleRequest) {
        ArtifactContextUtil<T> artifactContextUtil = new ArtifactContextUtil<>(getConfig().getArtifactRepo().resolve(moduleRequest.getFileName()));
        moduleRequest.setRestartCommand(artifactContextUtil.getRestartCommand());
        moduleRequest.setTestCommand(artifactContextUtil.getTestCommand());
        moduleRequest.setBaseLocation(artifactContextUtil.getBaseLocation());
        return just(moduleRequest);
    }

    default Observable<T> extractArtifact(T moduleRequest) {
        ExtractArtifact<T> extractConfig = new ExtractArtifact<>(getVertx(), getConfig(), moduleRequest.getBaseLocation());
        return extractConfig.executeAsync(moduleRequest);
    }


}
