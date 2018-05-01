package nl.jpoint.vertx.deploy.agent.command;

import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.request.ModuleRequest;
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

import static rx.Observable.just;

public class DownloadHttpArtifact<T extends ModuleRequest> implements Command<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadHttpArtifact.class);
    private final DeployConfig config;
    private String logConstant;

    public DownloadHttpArtifact(DeployConfig config, String logConstant) {
        this.config = config;
        this.logConstant = logConstant;
    }

    @Override
    public Observable<T> executeAsync(T request) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(config.getHttpAuthUser(), config.getHttpAuthPassword());
        provider.setCredentials(AuthScope.ANY, credentials);
        final URI location = config.getNexusUrl().resolve(config.getNexusUrl().getPath() + "/" + request.getRemoteLocation());
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build()) {
            LOG.info("[{} - {}]: Downloaded artifact {} to {}.", logConstant, request.getId(), request.getModuleId(), request.getLocalPath(config.getArtifactRepo()));
            HttpUriRequest get = new HttpGet(location);
            CloseableHttpResponse response = client.execute(get);
            response.getEntity().writeTo(new FileOutputStream(request.getLocalPath(config.getArtifactRepo()).toFile()));
        } catch (IOException e) {
            LOG.error("[{} - {}]: Error downloading artifact -> {}, {}", logConstant, request.getId(), e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        return just(request);
    }
}
