package nl.jpoint.vertx.deploy.agent.command;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.request.ModuleRequest;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import nl.jpoint.vertx.deploy.agent.util.MetadataXPathUtil;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(config.getHttpAuthUser(), config.getHttpAuthPassword());
        provider.setCredentials(AuthScope.ANY, credentials);

        final URI location = config.getNexusUrl().resolve(config.getNexusUrl().getPath() + "/" + request.getMetadataLocation());
        HttpUriRequest get = new HttpGet(location);

        Path filename = createTempFile(request.getArtifactId());

        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
             CloseableHttpResponse response = client.execute(get)) {
            LOG.info("[{} - {}]: Downloaded Metadata for {}.", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), request.getModuleId());
            response.getEntity().writeTo(new FileOutputStream(filename.toFile()));
            request.setVersion(retrieveAndParseMetadata(filename, request));
        } catch (IOException e) {
            LOG.error("[{} - {}]: Error downloading Metadata  -> {}, {}", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        return just(request);
    }

    private Path createTempFile(String filename) {
        return Paths.get(System.getProperty("java.io.tmpdir") + "/" + filename);
    }

    private String retrieveAndParseMetadata(Path fileLocation, ModuleRequest request) {
        Buffer b = rxVertx.fileSystem().readFileBlocking(fileLocation.toString());
        return MetadataXPathUtil.getRealSnapshotVersionFromMetadata(b.toString().getBytes(), request);
    }
}
