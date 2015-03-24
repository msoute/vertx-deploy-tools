package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.Request;
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
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestExecutor {

    private final Log log;
    private final long timeout;

    public RequestExecutor(Log log) {

        this.log = log;
        timeout = System.currentTimeMillis() + 60 * 100000;
    }

    private void executeAwsRequest(final HttpPost postRequest, final String host) throws MojoExecutionException, MojoFailureException {
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

                    HttpGet get = new HttpGet(postRequest.getURI().getScheme()+"://"+postRequest.getURI().getHost() +":"+postRequest.getURI().getPort()+ "/deploy/status/" + buildId);
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
                        if (status.get() != 200) {status.set(500);}
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
            if (status.get() != 200) {
                throw new MojoFailureException("Error deploying module.");
            }


        } catch (IOException e) {
            log.error("IOException ", e);
            throw new MojoExecutionException("Error deploying module.", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException ", e);
            throw new MojoExecutionException("Error deploying module.", e);
        }
    }

    private void executeRequest(HttpPost postRequest) throws MojoExecutionException {

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.info("Waiting for deploy request to return...");
            }
        }, 5, 5, TimeUnit.SECONDS);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                exec.shutdown();
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
        } finally {
            if (!exec.isShutdown()) {
                log.info("Shutdown executor after error");
                exec.shutdown();
            }
        }
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

    public void executeDeployRequests(DeployConfiguration activeConfiguration, DeployRequest deployRequest, Settings settings) throws MojoExecutionException, MojoFailureException {

        if (activeConfiguration.getOpsWorks() && activeConfiguration.getOpsWorksStackId() != null) {
            getHostsOpsWorks(activeConfiguration, settings);
        }

        if (activeConfiguration.isAutoScaling() && activeConfiguration.getAutoScalingGroupId() != null) {
            getHostsForAutoScalingGroup(activeConfiguration, settings);
        }
        
        for (String host : activeConfiguration.getHosts()) {

            log.info("Deploying to host : " + host);

            HttpPost post = new HttpPost(createDeployUri(host) + deployRequest.getEndpoint());
            ByteArrayInputStream bos = new ByteArrayInputStream(deployRequest.toJson().getBytes());
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(bos);
            entity.setContentLength(deployRequest.toJson().getBytes().length);
            post.setEntity(entity);



            if (!activeConfiguration.getAws()) {
                this.executeRequest(post);
            } else {
                this.executeAwsRequest(post, host);
            }

        }
    }

    private void getHostsForAutoScalingGroup(DeployConfiguration activeConfiguration, Settings settings) throws MojoFailureException {
        log.info("retrieving list of hosts for auto scaling group with id : " + activeConfiguration.getOpsWorksStackId());
        activeConfiguration.getHosts().clear();
        if (settings.getServer(activeConfiguration.getOpsWorksStackId())== null) {
            throw new MojoFailureException("No server config for auto scaling group id : " + activeConfiguration.getOpsWorksStackId());
        }
        Server server = settings.getServer(activeConfiguration.getOpsWorksStackId());
        AwsAutoScalingUtil awsAutoScalingUtil = new AwsAutoScalingUtil(server.getUsername(), server.getPassword());
        AwsEc2Util awsEc2Util = new AwsEc2Util(server.getUsername(), server.getPassword());
        List<String> instanceIds;
        List<String> hosts;
        try {
            instanceIds = awsAutoScalingUtil.listInstancesInGroup(activeConfiguration.getAutoScalingGroupId(), log);
            hosts = awsEc2Util.describeInstance(instanceIds, log);

            if (hosts.isEmpty()) {
                log.error("No hosts in AS group with id : " + activeConfiguration.getAutoScalingGroupId());
                throw new MojoFailureException("No hosts in AS group with id : " + activeConfiguration.getAutoScalingGroupId());
            }

            for (String opsHost : hosts) {
                log.info("Adding host from opsworks response : " + opsHost);
                activeConfiguration.getHosts().add("http://"+opsHost+":6789");
            }
        } catch (AwsException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void getHostsOpsWorks(DeployConfiguration activeConfiguration, Settings settings) throws MojoFailureException {
        log.info("retrieving list of hosts for stack with id : " + activeConfiguration.getOpsWorksStackId());
        activeConfiguration.getHosts().clear();
        if (settings.getServer(activeConfiguration.getOpsWorksStackId())== null) {
            throw new MojoFailureException("No server config for stack id : " + activeConfiguration.getOpsWorksStackId());
        }
        Server server = settings.getServer(activeConfiguration.getOpsWorksStackId());
        AwsOpsWorksUtil opsWorksUtil = new AwsOpsWorksUtil(server.getUsername(), server.getPassword());
        List<String> hosts;
        try {
            hosts = opsWorksUtil.ListStackInstances(activeConfiguration.getOpsWorksStackId(), activeConfiguration.getOpsWorksLayerId(), activeConfiguration.getAwsPrivateIp(), log);
            for (String opsHost : hosts) {
                log.info("Adding host from opsworks response : " + opsHost);
                activeConfiguration.getHosts().add("http://"+opsHost+":6789");
            }
        } catch (AwsException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private String createDeployUri(String host) {
        if (!host.startsWith("http://")) {
            host = "http://"+host;
        }
        if (!host.endsWith(":6789")) {
            host = host+":6789";
        }
        return host;
    }
}
