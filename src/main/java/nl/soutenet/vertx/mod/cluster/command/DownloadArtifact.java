package nl.soutenet.vertx.mod.cluster.command;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.soutenet.vertx.mod.cluster.request.ModuleRequest;
import nl.soutenet.vertx.mod.cluster.util.LogConstants;
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
        String reposFile = config.getString("vertx.home")+CONF_REPOS_TXT;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(reposFile))));

            String line = null;
            remoteRepositories = new ArrayList<>();
            while((line = reader.readLine()) != null) {
                if (line.startsWith("maven:")) {
                    remoteRepositories.add(line.substring(6));
                }
            }
        } catch (IOException e) {
            LOG.error("[{}] : Error initializing remote repositories {}.", LogConstants.DEPLOY_SITE_REQUEST, e.getMessage());
        }
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        credsProvider.setCredentials(
                new AuthScope("build.edubase.malmberg.nl", 443),
                new UsernamePasswordCredentials("build", "CragDiv1"));

        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        boolean downloaded = false;

        Iterator<String> it = remoteRepositories.iterator();
        while (it.hasNext() && !downloaded) {
            String uri = it.next();
            System.out.println(uri+"/"+request.getRemoteLocation());
            HttpGet get = new HttpGet(uri+"/"+request.getRemoteLocation());

            try(CloseableHttpResponse response = httpclient.execute(get)) {

                if (response.getStatusLine().getStatusCode() == HttpResponseStatus.OK.code()) {
                    FileOutputStream fos = new FileOutputStream(new File("target/test.zip"));
                    response.getEntity().writeTo(fos);
                    response.close();
                    fos.close();
                } else {
                    LOG.error("[{} - {}] : Error downloading artifact {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId());
                    LOG.error("[{} - {}] : HttpClient Error [{}] -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(),response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                LOG.error("[{} - {}] : Error downloading artifact {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getArtifactId());
            }

        }
        return null;
    }
}
