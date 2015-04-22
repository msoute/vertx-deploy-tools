package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestExecutor {

    private final Log log;
    private final long timeout;

    public RequestExecutor(Log log, Integer requestTimeout) {
        this.log = log;
        timeout = System.currentTimeMillis() + (60000L * requestTimeout);
        log.info("Setting timeout to : " + new Date(timeout));
    }

    private AwsState executeAwsRequest(final HttpPost postRequest, final boolean ignoreFailure) throws MojoExecutionException, MojoFailureException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String buildId;
            final AtomicInteger waitFor = new AtomicInteger(1);
            final AtomicInteger status = new AtomicInteger(0);
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.error("DeployModuleCommand : Post response status -> " + response.getStatusLine().getReasonPhrase());
                    throw new MojoExecutionException("Error deploying module. ");
                }

                buildId = EntityUtils.toString(response.getEntity());
            }

            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();


            exec.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {

                    HttpGet get = new HttpGet(postRequest.getURI().getScheme() + "://" + postRequest.getURI().getHost() + ":" + postRequest.getURI().getPort() + "/deploy/status/" + buildId);
                    try (CloseableHttpResponse response = httpClient.execute(get)) {
                        int code = response.getStatusLine().getStatusCode();
                        String state = response.getStatusLine().getReasonPhrase();
                        switch (code) {
                            case 200:
                                log.info("Deploy request finished executing");
                                status.set(200);
                                waitFor.decrementAndGet();
                                break;
                            case 500:
                                if (status.get() != 200) {
                                    status.set(500);
                                    log.error("Deploy request failed");
                                    waitFor.decrementAndGet();
                                }
                                break;
                            default:
                                if (System.currentTimeMillis() > timeout) {
                                    if (status.get() != 200) {
                                        status.set(500);
                                    }
                                    log.error("Timeout while waiting for deploy request.");
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
                }
            }, 0, 15, TimeUnit.SECONDS);

            while (waitFor.intValue() > 0) {
                Thread.sleep(15000);
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

    private AwsState executeRequest(final HttpPost postRequest) throws MojoExecutionException {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.info("Waiting for deploy request to return...");
            }
        }, 5, 5, TimeUnit.SECONDS);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    postRequest.abort();
                }
            }, timeout);
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                exec.shutdown();
                log.info("DeployModuleCommand : Post response status code -> " + response.getStatusLine().getStatusCode());
                if (postRequest.isAborted()) {
                    log.error("Timeout while waiting for deploy request, aborting request");
                    throw new MojoExecutionException("Timeout while waiting for deploy request, aborting request");
                }
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
        } finally {
            if (!exec.isShutdown()) {
                log.info("Shutdown executor after error");
                exec.shutdown();
            }
        }
        return AwsState.INSERVICE;
    }

    public void executeSingleDeployRequest(DeployConfiguration activeConfiguration, Request request) throws MojoExecutionException {
        for (String host : activeConfiguration.getHosts()) {
            log.info("Deploying to host : " + host);
            HttpPost post = new HttpPost(createDeployUri(host) + request.getEndpoint());
            ByteArrayInputStream bos = new ByteArrayInputStream(request.toJson().getBytes());
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(bos);
            entity.setContentLength(request.toJson().getBytes().length);
            post.setEntity(entity);
            this.executeRequest(post);
        }
    }


    public AwsState executeAwsDeployRequest(DeployRequest deployRequest, String host, boolean ignoreFailure) throws MojoFailureException, MojoExecutionException {
        return executeRequest(deployRequest, host, true, ignoreFailure);
    }

    public void executeDeployRequest(DeployRequest deployRequest, String host) throws MojoFailureException, MojoExecutionException {
        executeRequest(deployRequest, host, false, false);
    }

    private AwsState executeRequest(DeployRequest deployRequest, String host, boolean withAws, boolean ignoreFailure) throws MojoExecutionException, MojoFailureException {
        log.info("Deploying to host : " + host);
        HttpPost post = new HttpPost(createDeployUri(host) + deployRequest.getEndpoint());
        ByteArrayInputStream bos = new ByteArrayInputStream(deployRequest.toJson(false).getBytes());
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(bos);
        entity.setContentLength(deployRequest.toJson(false).getBytes().length);
        post.setEntity(entity);

        if (!withAws) {
            return this.executeRequest(post);
        } else {
            return this.executeAwsRequest(post, ignoreFailure);
        }
    }

    private String createDeployUri(String host) {
        if (!host.startsWith("http://")) {
            host = "http://" + host;
        }
        if (!host.endsWith(":6789")) {
            host = host + ":6789";
        }
        return host;
    }


}
