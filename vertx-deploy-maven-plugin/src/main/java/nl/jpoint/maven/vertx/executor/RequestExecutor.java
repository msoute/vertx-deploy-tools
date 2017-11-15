package nl.jpoint.maven.vertx.executor;


import com.amazonaws.util.StringUtils;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.utils.AwsState;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayInputStream;
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
        log.info("Deploying to host : " + host);
        HttpPost post = new HttpPost(createDeployUri(host) + deployRequest.getEndpoint());
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


    private String createDeployUri(String host) {
        String mappedHost = null;
        if (!host.startsWith("http://")) {
            mappedHost = "http://" + host;
        }
        if (!host.endsWith(Integer.toString(port))) {
            mappedHost = host + ":" + port;
        }
        return mappedHost != null ? mappedHost : host;
    }

    long getTimeout() {
        return timeout;
    }


    public abstract AwsState executeRequest(DeployRequest deployRequest, String host, boolean ignoreFailure) throws MojoExecutionException, MojoFailureException;
}
