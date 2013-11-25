package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class RequestExecutor {

    private final Log log;

    public RequestExecutor(Log log) {

        this.log = log;
    }

    private void executeRequest(HttpPost postRequest) throws MojoExecutionException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                log.info("DeployModuleCommand : Post response status code -> " + response.getStatusLine().getStatusCode());

                if (response.getStatusLine().getStatusCode() != 200) {
                    log.error("DeployModuleCommand : Post response status -> " + response.getStatusLine().getReasonPhrase());
                    throw new MojoExecutionException("Error deploying module. ");
                }
            } catch (IOException e) {
                log.error("testDeployModuleCommand ", e);
                throw new MojoExecutionException("Error deploying module.", e);
            }

        } catch (IOException e) {
            log.error("testDeployModuleCommand ", e);
            throw new MojoExecutionException("Error deploying module.", e);
        }
    }

    public void executeSingleDeployRequest(DeployConfiguration activeConfiguration, Request request) throws MojoExecutionException {
        for (String host : activeConfiguration.getHosts()) {
            HttpPost post = new HttpPost(host + request.getEndpoint());

            ByteArrayInputStream bos = new ByteArrayInputStream(request.toJson().getBytes());
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(bos);
            entity.setContentLength(request.toJson().getBytes().length);
            post.setEntity(entity);

            this.executeRequest(post);
        }
    }

    public void executeDeployRequests(DeployConfiguration activeConfiguration, DeployRequest deployRequest) throws MojoExecutionException {

        for (String host : activeConfiguration.getHosts()) {

                HttpPost post = new HttpPost(host + deployRequest.getEndpoint());

                ByteArrayInputStream bos = new ByteArrayInputStream(deployRequest.toJson().getBytes());
                BasicHttpEntity entity = new BasicHttpEntity();
                entity.setContent(bos);
                entity.setContentLength(deployRequest.toJson().getBytes().length);
                post.setEntity(entity);
                log.info(deployRequest.toJson());
                this.executeRequest(post);

        }
    }
}
