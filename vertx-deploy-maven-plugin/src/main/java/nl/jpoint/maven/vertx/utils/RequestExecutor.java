package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class RequestExecutor {

    private final Log log;

    public RequestExecutor(Log log) {

        this.log = log;
    }

    public void executeDeployRequests(DeployConfiguration activeConfiguration, List<Request> requestList) {

        for (String host : activeConfiguration.getHosts()) {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            for (Request request : requestList) {
                HttpPost post = new HttpPost(host + request.getEndpoint());

                ByteArrayInputStream bos = new ByteArrayInputStream(request.toJson().getBytes());
                BasicHttpEntity entity = new BasicHttpEntity();
                entity.setContent(bos);
                entity.setContentLength(request.toJson().getBytes().length);
                post.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    log.info("DeployModuleCommand : Post response status " + response.getStatusLine().getStatusCode());
                } catch (IOException e) {
                    log.error("testDeployModuleCommand ", e);
                }
            }

        }
    }
}
