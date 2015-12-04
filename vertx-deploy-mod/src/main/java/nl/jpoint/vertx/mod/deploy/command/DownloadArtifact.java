package nl.jpoint.vertx.mod.deploy.command;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class DownloadArtifact implements Command<ModuleRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadArtifact.class);
    private final DeployConfig config;

    public DownloadArtifact(DeployConfig config) {
        this.config = config;
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        if (config.isHttpAuthentication()) {
            credsProvider.setCredentials(
                    new AuthScope(config.getNexusUrl().getHost(), "https".equals(config.getNexusUrl().getScheme()) ? 443 : 80),
                    new UsernamePasswordCredentials(config.getHttpAuthUser(), config.getHttpAuthPassword()));
        }

        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

        boolean downloaded = false;
        HttpGet get = new HttpGet(config.getNexusUrl().resolve(request.getRemoteLocation()));

        try (CloseableHttpResponse response = httpclient.execute(get)) {

            if (response.getStatusLine().getStatusCode() == HttpResponseStatus.OK.code()) {
                OutputStream fos = new BufferedOutputStream(new FileOutputStream(new File(config.getArtifactRepo() + request.getFileName())));
                response.getEntity().writeTo(fos);
                response.close();
                fos.close();
                LOG.info("[{} - {}]: Downloaded artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), config.getArtifactRepo() + request.getModuleId() + "." + request.getType());
                downloaded = true;
            } else {
                LOG.error("[{} - {}]: Error downloading artifact {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId());
                LOG.error("[{} - {}]: HttpClient Error [{}] -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            LOG.error("[{} - {}]: IOException while Error downloading artifact {}. Reason '{}'", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getArtifactId(), e.getMessage());
        }
        return new JsonObject().put("success", downloaded);
    }
}
