package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClientRequest;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;

import java.util.List;

public class AutoDiscoverDeployService {
    private final Vertx rxVertx;
    private final DeployConfig deployConfig;

    public AutoDiscoverDeployService(io.vertx.core.Vertx vertx, DeployConfig config) {
        this.deployConfig = config;
        this.rxVertx = new Vertx(vertx);
    }

    public void autoDiscoverFirstDeploy() {
        // retreive all latest deployRequests from ASG group instances
        List<DeployRequest> clusterDeployRequests = null;
        // Get Latest successful one
        DeployRequest latestSuccessfulRequest = null;

        if (latestSuccessfulRequest != null) {
            HttpClientRequest request = rxVertx.createHttpClient().postAbs("http://localhost:" + deployConfig.getHttpPort() + "/deploy/deploy");

            request.putHeader("authToken", deployConfig.getAuthToken());
            request.end("deployRequest");
        }
    }
}