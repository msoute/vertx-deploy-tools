package nl.jpoint.vertx.mod.deploy.command;

import io.netty.handler.codec.http.HttpResponseStatus;
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
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DownloadArtifact implements Command<ModuleRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadArtifact.class);
    private static final String CONF_REPOS_TXT = "/conf/repos.txt";
    private final JsonObject config;
    private List<String> remoteRepositories;

    public DownloadArtifact(JsonObject config) {
        this.config = config;
        this.initializeRepoList();
    }

    private void initializeRepoList() {
        String reposFile = config.getString("vertx.home") + CONF_REPOS_TXT;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(reposFile))));

            String line;
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

        boolean secure = true;

        if (config.containsField("http.authSecure")) {
            secure = config.getBoolean("http.authSecure");
        }

        credsProvider.setCredentials(
                new AuthScope(config.getString("http.authUri"), secure ? 443 : 80),
                new UsernamePasswordCredentials(config.getString("http.authUser"), config.getString("http.authPass")));


        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

        boolean downloaded = false;

        Iterator<String> it = remoteRepositories.iterator();
        while (it.hasNext() && !downloaded) {
            String uri = it.next();
            HttpGet get = new HttpGet(uri + "/" + request.getRemoteLocation());

            try (CloseableHttpResponse response = httpclient.execute(get)) {

                if (response.getStatusLine().getStatusCode() == HttpResponseStatus.OK.code()) {
                    OutputStream fos = new BufferedOutputStream(new FileOutputStream(new File(config.getString("artifact.repo") + request.getFileName())));
                    response.getEntity().writeTo(fos);
                    response.close();
                    fos.close();
                    LOG.info("[{} - {}]: Downloaded artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), config.getString("artifact.repo") + request.getModuleId() + "." + request.getType());
                    downloaded = true;
                } else {
                    LOG.error("[{} - {}]: Error downloading artifact {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId());
                    LOG.error("[{} - {}]: HttpClient Error [{}] -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                LOG.error("[{} - {}]: IOException while Error downloading artifact {}. Reason '{}'", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getArtifactId(), e.getMessage());
            }
        }
        return new JsonObject().putBoolean("success", downloaded);
    }
}
