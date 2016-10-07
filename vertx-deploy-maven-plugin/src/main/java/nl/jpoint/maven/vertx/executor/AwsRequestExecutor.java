package nl.jpoint.maven.vertx.executor;

import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.utils.AwsState;
import nl.jpoint.maven.vertx.utils.LogUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AwsRequestExecutor extends RequestExecutor {

    public AwsRequestExecutor(Log log, Integer requestTimeout, Integer port, String authToken) {
        super(log, requestTimeout, port, authToken);
    }

    @Override
    public AwsState executeRequest(DeployRequest deployRequest, String host, boolean ignoreFailure) throws MojoExecutionException, MojoFailureException {
        return executeAwsRequest(createPost(deployRequest, host), ignoreFailure);
    }

    private AwsState executeAwsRequest(final HttpPost postRequest, final boolean ignoreFailure) throws MojoExecutionException, MojoFailureException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String buildId;
            final AtomicInteger waitFor = new AtomicInteger(1);
            final AtomicInteger status = new AtomicInteger(0);
            final AtomicBoolean errorLogged = new AtomicBoolean(false);
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.error("DeployModuleCommand : Post response status -> " + response.getStatusLine().getReasonPhrase());
                    throw new MojoExecutionException("Error deploying module. ");
                }

                buildId = EntityUtils.toString(response.getEntity());
            }

            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

            final RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(5000)
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000)
                    .build();

            exec.scheduleAtFixedRate(() -> {
                HttpGet get = new HttpGet(postRequest.getURI().getScheme() + "://" + postRequest.getURI().getHost() + ":" + postRequest.getURI().getPort() + "/deploy/status/" + buildId);
                get.setConfig(requestConfig);
                try (CloseableHttpResponse response = httpClient.execute(get)) {
                    int code = response.getStatusLine().getStatusCode();
                    String state = response.getStatusLine().getReasonPhrase();
                    switch (code) {
                        case 200:
                            log.info("Deploy request finished executing");
                            LogUtil.logDeployResult(log, EntityUtils.toString(response.getEntity()));
                            status.set(200);
                            waitFor.decrementAndGet();
                            break;
                        case 500:
                            if (status.get() != 200) {
                                status.set(500);
                                if (!errorLogged.getAndSet(true)) {
                                    LogUtil.logDeployResult(log, EntityUtils.toString(response.getEntity()));
                                    waitFor.decrementAndGet();
                                }
                            }
                            break;
                        default:
                            if (System.currentTimeMillis() > getTimeout()) {
                                if (status.get() != 200) {
                                    status.set(500);
                                }
                                log.error("Timeout while waiting for deploy request.");
                                LogUtil.logDeployResult(log, EntityUtils.toString(response.getEntity()));
                                waitFor.decrementAndGet();
                            }
                            log.info("Waiting for deploy to finish. Current status : " + state);
                            break;
                    }

                } catch (IOException e) {
                    if (status.get() != 200) {
                        status.set(500);
                    }
                    waitFor.decrementAndGet();
                }
            }, 0, 2, TimeUnit.SECONDS);

            while (waitFor.intValue() > 0) {
                Thread.sleep(3000);
            }

            log.info("Shutting down executor");
            exec.shutdown();
            log.info("awaiting termination of executor");
            exec.awaitTermination(30, TimeUnit.SECONDS);
            if (status.get() != 200 && !ignoreFailure) {
                throw new MojoFailureException("Error deploying module.");
            }
            return status.get() == 200 ? AwsState.INSERVICE : AwsState.UNKNOWN;
        } catch (IOException e) {
            log.error("IOException ", e);
            throw new MojoExecutionException("Error deploying module.", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException ", e);
            throw new MojoExecutionException("Error deploying module.", e);
        }
    }
}
