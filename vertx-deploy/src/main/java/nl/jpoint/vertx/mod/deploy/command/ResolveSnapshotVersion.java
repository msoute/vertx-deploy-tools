package nl.jpoint.vertx.mod.deploy.command;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.MetadataXPathUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class ResolveSnapshotVersion implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(ResolveSnapshotVersion.class);
    private final DeployConfig config;
    private final String logId;

    public ResolveSnapshotVersion(DeployConfig config, String logId) {

        this.config = config;
        this.logId = logId;
    }

    @Override
    public JsonObject execute(ModuleRequest request) {


        HttpClient httpclient = new DefaultHttpClient();


        boolean resolved = false;
        String realSnapshotVersion = request.isSnapshot() ? request.getVersion() : null;

        if (request.isSnapshot()) {
            LOG.info("[{} - {}]: Artifact is -SNAPSHOT, trying to parse metadata for last version {}.", logId, request.getId(), request.getModuleId());
            realSnapshotVersion = this.retrieveAndParseMetadata(request, httpclient, config.getNexusUrl());
            if (realSnapshotVersion != null) {
                LOG.info("[{} - {}]: Parsed metadata. Snapshot version is {} ", logId, request.getId(), realSnapshotVersion);
                resolved = true;
            }
        }
        return new JsonObject().put("success", resolved).put("version", realSnapshotVersion);

    }

    private String retrieveAndParseMetadata(ModuleRequest request, HttpClient httpclient, URI repoUri) {
        HttpGet getMetadata = new HttpGet(repoUri.resolve(repoUri.getPath() + "/" + request.getMetadataLocation()));
        if (config.isHttpAuthentication()) {
            setAuthorizationHeader(getMetadata);
        }

        try {
            HttpResponse response = httpclient.execute(getMetadata);
            if (response.getStatusLine().getStatusCode() != HttpResponseStatus.OK.code()) {
                LOG.error("[{} - {}]: No metadata found for module {} with error code {} with request {}", logId, request.getId(), request.getModuleId(), response.getStatusLine().getStatusCode(), getMetadata.getURI());
                return null;
            }
            byte[] metadata = EntityUtils.toByteArray(response.getEntity());
            return MetadataXPathUtil.getRealSnapshotVersionFromMetadata(metadata, request);

        } catch (IOException e) {
            LOG.error("[{} - {}]: Error while downloading metadata for module {} : {}", logId, request.getId(), request.getModuleId(), e.getMessage());
            return null;
        }
    }

    private void setAuthorizationHeader(HttpGet httpClientRequest) {
        String usernameAndPassword = config.getHttpAuthUser() + ":" + config.getHttpAuthPassword();
        String authorizationHeaderName = "Authorization";
        String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
        httpClientRequest.addHeader(authorizationHeaderName, authorizationHeaderValue);
    }
}
