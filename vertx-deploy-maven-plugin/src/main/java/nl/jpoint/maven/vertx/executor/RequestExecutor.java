package nl.jpoint.maven.vertx.executor;


import com.amazonaws.util.StringUtils;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.utils.AwsState;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

public abstract class RequestExecutor {
    final Log log;
    private final Integer port;
    private final String authToken;
    private final long timeout;

    RequestExecutor(Log log, Integer requestTimeout, Integer port, String authToken) {
        this.log = log;
        this.port = port;
        this.authToken = authToken != null ? authToken : "";
        this.timeout = System.currentTimeMillis() + (60000L * requestTimeout);
        log.info("Setting timeout to : " + new Date(timeout));
    }

    HttpPost createPost(DeployRequest deployRequest, String host) {
        URI deployUri = createDeployUri(host, deployRequest.getEndpoint());
        log.info("Deploying to host : " + deployUri.toString());
        HttpPost post = new HttpPost(deployUri);
        if (!StringUtils.isNullOrEmpty(authToken)) {
            log.info("Adding authToken to request header.");
            post.addHeader("authToken", authToken);
        }

        ByteArrayInputStream bos = new ByteArrayInputStream(deployRequest.toJson(false).getBytes());
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(bos);
        entity.setContentLength(deployRequest.toJson(false).getBytes().length);
        post.setEntity(entity);
        return post;
    }


    private URI createDeployUri(String host, String endpoint) {
        try {
            return new URIBuilder().setScheme("http").setHost(host).setPort(port).setPath(endpoint).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

    }

    long getTimeout() {
        return timeout;
    }


    public abstract AwsState executeRequest(DeployRequest deployRequest, String host, boolean ignoreFailure) throws MojoExecutionException, MojoFailureException;
}
