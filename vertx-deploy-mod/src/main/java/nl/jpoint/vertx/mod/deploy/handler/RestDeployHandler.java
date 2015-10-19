package nl.jpoint.vertx.mod.deploy.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import nl.jpoint.vertx.mod.deploy.service.DeployModuleService;
import nl.jpoint.vertx.mod.deploy.service.DeployService;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;

public class RestDeployHandler implements Handler<HttpServerRequest> {

    private final DeployService<DeployModuleRequest> moduleDeployService;
    private final DeployService<DeployArtifactRequest> artifactDeployService;
    private final DeployService<DeployConfigRequest> configDeployService;
    private final AwsService awsService;

    private final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);

    public RestDeployHandler(final DeployService<DeployModuleRequest> moduleDeployService,
                             final DeployService<DeployArtifactRequest> artifactDeployService,
                             final DeployService<DeployConfigRequest> configDeployService,
                             final AwsService awsService) {
        MDC.put("service", Constants.SERVICE_ID);
        this.moduleDeployService = moduleDeployService;
        this.artifactDeployService = artifactDeployService;
        this.configDeployService = configDeployService;
        this.awsService = awsService;
    }

    @Override
    public void handle(final HttpServerRequest request) {
        request.bodyHandler(event -> {
            ObjectReader reader = new ObjectMapper().reader(DeployRequest.class);

            DeployRequest deployRequest;

            if (event.getBytes() == null || event.getBytes().length == 0) {
                LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_REQUEST);
                respondFailed(request);
                return;
            }
            byte[] eventBody = event.getBytes();
            LOG.debug("{}: received postdata size  -> {} ", LogConstants.DEPLOY_REQUEST, eventBody.length);
            LOG.debug("{}: received postdata -> {} ", LogConstants.DEPLOY_REQUEST, new String(eventBody));
            try {
                deployRequest = reader.readValue(event.getBytes());
            } catch (IOException e) {
                LOG.error("{}: Error while reading postdata -> {}.", LogConstants.DEPLOY_REQUEST, e.getMessage());
                respondFailed(request);
                return;
            }

            LOG.info("[{} - {}]: Received deploy request with {} config(s), {} module(s) and {} artifact(s) ", LogConstants.DEPLOY_REQUEST,
                    deployRequest.getId().toString(),
                    deployRequest.getConfigs() != null ? deployRequest.getConfigs().size() : 0,
                    deployRequest.getModules() != null ? deployRequest.getModules().size() : 0,
                    deployRequest.getArtifacts() != null ? deployRequest.getArtifacts().size() : 0);

            JsonObject deployOk = null;


            if (deployRequest.withElb()) {
                if (awsService.registerRequest(deployRequest)) {
                    respondContinue(request, deployRequest.getId().toString());
                    awsService.deRegisterInstance(deployRequest.getId().toString());
                } else {
                    respondFailed(request);
                }
                return;
            }

            if (deployRequest.withRestart()) {
                ((DeployModuleService) moduleDeployService).stopContainer(deployRequest.getId().toString());
            }

            if (deployRequest.getConfigs() != null && !deployRequest.getConfigs().isEmpty()) {
                for (DeployConfigRequest configRequest : deployRequest.getConfigs()) {
                    deployOk = configDeployService.deploy(configRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(request);
                        return;
                    }
                }

                if (deployRequest.withRestart() && deployOk != null && deployOk.getBoolean("configChanged", false)) {
                    ((DeployModuleService) moduleDeployService).stopContainer(deployRequest.getId().toString());
                    deployRequest.setRestart(true);
                }
            }

            if (deployRequest.getArtifacts() != null && !deployRequest.getArtifacts().isEmpty()) {
                for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                    deployOk = artifactDeployService.deploy(artifactRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(request);
                        return;
                    }
                }
            }

            if (deployRequest.getModules() != null && !deployRequest.getModules().isEmpty()) {
                for (DeployModuleRequest moduleRequest : deployRequest.getModules()) {
                    deployOk = moduleDeployService.deploy(moduleRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(request);
                        return;
                    }
                }
            }

            respondOk(request);
        });
    }

    private void respondOk(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        request.response().end();
    }

    private void respondContinue(HttpServerRequest request, String id) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        request.response().setStatusMessage(id);
        request.response().end(id);
    }

    private void respondFailed(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        request.response().end();
    }
}
