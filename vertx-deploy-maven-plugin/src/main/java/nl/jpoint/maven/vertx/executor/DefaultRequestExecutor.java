package nl.jpoint.maven.vertx.executor;

import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.utils.AwsState;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultRequestExecutor extends RequestExecutor {

    public DefaultRequestExecutor(Log log, Integer requestTimeout, Integer port, String authToken) {
        super(log, requestTimeout, port, authToken);
    }


    private AwsState executeRequest(final HttpPost postRequest) throws MojoExecutionException {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> log.info("Waiting for deploy request to return..."), 5, 5, TimeUnit.SECONDS);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    postRequest.abort();
                }
            }, getTimeout());
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                exec.shutdown();
                log.info("DeployModuleCommand : Post response status code -> " + response.getStatusLine().getStatusCode());
                if (postRequest.isAborted()) {
                    log.error("Timeout while waiting for deploy request, aborting request");
                    throw new MojoExecutionException("Timeout while waiting for deploy request, aborting request");
                }
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.error("DeployModuleCommand : Post response status -> " + response.getStatusLine().getReasonPhrase());
                    log.error("DeployModuleCommand : " + EntityUtils.toString(response.getEntity()));
                    throw new MojoExecutionException("Error deploying module. ");
                }
            } catch (IOException e) {
                log.error("testDeployModuleCommand ", e);
                throw new MojoExecutionException("Error deploying module.", e);
            }
        } catch (IOException e) {
            log.error("testDeployModuleCommand ", e);
            throw new MojoExecutionException("Error deploying module.", e);
        } finally {
            if (!exec.isShutdown()) {
                log.info("Shutdown executor after error");
                exec.shutdown();
            }
        }
        return AwsState.INSERVICE;
    }

    @Override
    public AwsState executeRequest(DeployRequest deployRequest, String host, boolean ignoreFailure) throws MojoExecutionException, MojoFailureException {
        return this.executeRequest(createPost(deployRequest, host));

    }


}
