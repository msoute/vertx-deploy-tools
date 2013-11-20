package nl.jpoint.vertx.mod.cluster.command;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import nl.jpoint.vertx.mod.cluster.util.MetadataXPathUtil;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DownloadArtifact implements Command {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadArtifact.class);

    private static final String CONF_REPOS_TXT = "/conf/repos.txt";
    private List<String> remoteRepositories;

    private final JsonObject config;

    public DownloadArtifact(JsonObject config) {
        this.config = config;
        this.initializeRepoList();
    }

    private void initializeRepoList() {
        String reposFile = config.getString("vertx.home") + CONF_REPOS_TXT;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(reposFile))));

            String line = null;
            remoteRepositories = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("maven:")) {
                    remoteRepositories.add(line.substring(6));
                }
            }
        } catch (IOException e) {
            LOG.error("[{}]: Error initializing remote repositories {}.", LogConstants.DEPLOY_SITE_REQUEST, e.getMessage());
        }
        if (remoteRepositories.size() == 0) {
            LOG.error("[{}]: No remote repositories initialized {}.", LogConstants.DEPLOY_SITE_REQUEST);
        }
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        credsProvider.setCredentials(
               //new AuthScope(null, -1),
               new AuthScope(config.getString("http.authUri"), 443),
                new UsernamePasswordCredentials(config.getString("http.authUser"), config.getString("http.authPass")));


        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

        boolean downloaded = false;

        Iterator<String> it = remoteRepositories.iterator();
        while (it.hasNext() && !downloaded) {
            String uri = it.next();
            String remoteLocation;
            if (request.isSnapshot()) {
                LOG.info("[{} - {}]: Artifact is -SNAPSHOT, trying to parse metadata for last version {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId());
                remoteLocation = this.retrieveAndParseMetadata(request, httpclient, uri);
                LOG.info("[{} - {}]: Parsed metadata for. Remote location is {} ", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), remoteLocation);
            } else {
                remoteLocation = request.getRemoteLocation();
            }
            HttpGet get = new HttpGet(uri + "/" + remoteLocation);

            try (CloseableHttpResponse response = httpclient.execute(get)) {

                if (response.getStatusLine().getStatusCode() == HttpResponseStatus.OK.code()) {
                    OutputStream fos = new BufferedOutputStream(new FileOutputStream(new File(config.getString("artifact.repo") + request.getModuleId() + ".zip")));
                    response.getEntity().writeTo(fos);
                    response.close();
                    fos.close();
                    LOG.info("[{} - {}]: Downloaded artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), config.getString("artifact.repo") + request.getModuleId() + ".zip");
                    downloaded = true;
                } else {
                    LOG.error("[{} - {}]: Error downloading artifact {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId());
                    LOG.error("[{} - {}]: HttpClient Error [{}] -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                LOG.error("[{} - {}]: Error downloading artifact {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getArtifactId());
            }
        }
        return new JsonObject().putBoolean("success", downloaded);
    }

    private String retrieveAndParseMetadata(ModuleRequest request, CloseableHttpClient httpclient, String repoUri) {
        HttpGet getMetadata = new HttpGet(repoUri + "/" + request.getMetadataLocation());
        try (CloseableHttpResponse response = httpclient.execute(getMetadata)) {
            if (response.getStatusLine().getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
                LOG.error("[{} - {}]: Not metadata found for module {}. Returning default remote location", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId());
                return request.getRemoteLocation();
            }
            byte[] metadata = EntityUtils.toByteArray(response.getEntity());
            response.close();
            return MetadataXPathUtil.getArtifactIdFromMetadata(metadata, request);
        } catch (IOException e) {
            LOG.error("[{} - {}]: Error while downloading metadata for module {} : {}", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), e.getMessage());
            return request.getRemoteLocation();
        }
    }
}
