package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.MetadataXPathUtil;
import nl.jpoint.vertx.mod.deploy.util.RxHttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URI;

import static rx.Observable.just;


public class ResolveSnapshotVersion<T extends ModuleRequest> implements Command<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ResolveSnapshotVersion.class);
    private final DeployConfig config;
    private final Vertx rxVertx;

    public ResolveSnapshotVersion(DeployConfig config, io.vertx.core.Vertx vertx) {
        this.rxVertx = new Vertx(vertx);
        this.config = config;
    }

    public Observable<T> executeAsync(T request) {
        final URI location = config.getNexusUrl().resolve(config.getNexusUrl().getPath() + "/" + request.getMetadataLocation());
        String filename = createTempFile(request.getArtifactId());
        return new RxHttpUtil(rxVertx, config).get(request.getId(), location, filename)
                .flatMap(x -> {
                    request.setVersion(retrieveAndParseMetadata(filename, request));
                    return just(request);
                }).onErrorReturn((x) -> {
                    LOG.error("[{} - {}]: Error downloading artifact {} for url {} -> .", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), request.getModuleId(), location, x);
                    return request;
                });
    }

    private String createTempFile(String filename) {
        return System.getProperty("java.io.tmpdir") + "/" + filename;
    }

    private String retrieveAndParseMetadata(String fileLocation, ModuleRequest request) {
        Buffer b = rxVertx.fileSystem().readFileBlocking(fileLocation);
        return MetadataXPathUtil.getRealSnapshotVersionFromMetadata(b.toString().getBytes(), request);
    }
}
