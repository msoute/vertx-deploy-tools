package nl.jpoint.vertx.mod.deploy.command;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.RxHttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URI;

import static rx.Observable.just;

public class DownloadArtifact<T extends ModuleRequest> implements Command<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadArtifact.class);
    private final DeployConfig config;
    private final Vertx rxVertx;

    public DownloadArtifact(DeployConfig config, io.vertx.core.Vertx vertx) {
        this.config = config;
        this.rxVertx = new Vertx(vertx);
    }

    @Override
    public Observable<T> executeAsync(T request) {
        final URI location = config.getNexusUrl().resolve(config.getNexusUrl().getPath() + "/" + request.getRemoteLocation());
        return new RxHttpUtil(rxVertx, config).get(location, config.getArtifactRepo() + "/" + request.getFileName())
                .flatMap(x -> {
                    if (x.statusCode() != HttpResponseStatus.OK.code()) {
                        LOG.error("[{} - {}]: Error downloading artifact {} for url {}.", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), request.getModuleId(), location);
                        LOG.error("[{} - {}]: HttpClient Error [{}] -> {}.", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), x.statusCode(), x.statusMessage());
                        throw new IllegalStateException();
                    }
                    LOG.info("[{} - {}]: Downloaded artifact {} to {}.", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), request.getModuleId(), config.getArtifactRepo() + request.getModuleId() + "." + request.getType());
                    return just(request);
                })
                .flatMap(x -> just(request));

    }
}
