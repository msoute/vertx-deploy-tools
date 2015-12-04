package nl.jpoint.vertx.mod.deploy.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
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

import java.io.IOException;

public class RestDeployHandler implements Handler<RoutingContext> {

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
    public void handle(final RoutingContext context) {
        context.addBodyEndHandler(aVoid -> {
            ObjectReader reader = new ObjectMapper().readerFor(DeployRequest.class);

            DeployRequest deployRequest;
            String eventBody = context.getBodyAsString();

            if (eventBody == null || eventBody.isEmpty()) {
                LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_REQUEST);
                respondFailed(context.request());
                return;
            }
            LOG.debug("{}: received postdata -> {} ", LogConstants.DEPLOY_REQUEST, eventBody);
            try {
                deployRequest = reader.readValue(eventBody);
            } catch (IOException e) {
                LOG.error("{}: Error while reading postdata -> {}.", LogConstants.DEPLOY_REQUEST, e.getMessage());
                respondFailed(context.request());
                return;
            }

            LOG.info("[{} - {}]: Received deploy request with {} config(s), {} module(s) and {} artifact(s) ", LogConstants.DEPLOY_REQUEST,
                    deployRequest.getId().toString(),
                    deployRequest.getConfigs() != null ? deployRequest.getConfigs().size() : 0,
                    deployRequest.getModules() != null ? deployRequest.getModules().size() : 0,
                    deployRequest.getArtifacts() != null ? deployRequest.getArtifacts().size() : 0);

            JsonObject deployOk = null;


            if (deployRequest.withElb()) {
                if (awsService != null && awsService.registerRequest(deployRequest)) {
                    respondContinue(context.request(), deployRequest.getId().toString());
                    awsService.deRegisterInstance(deployRequest.getId().toString());
                } else {
                    LOG.error("{}: Failed to register aws request or aws service disabled.", LogConstants.DEPLOY_REQUEST);
                    respondFailed(context.request());
                }
                return;
            }

            if (deployRequest.getConfigs() != null && !deployRequest.getConfigs().isEmpty()) {
                for (DeployConfigRequest configRequest : deployRequest.getConfigs()) {
                    deployOk = configDeployService.deploy(configRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(context.request());
                        return;
                    }
                    if (!deployRequest.withRestart() && deployOk.getBoolean("configChanged", false)) {
                        deployRequest.setRestart(true);
                    }
                }
            }

            if (deployRequest.withRestart()) {
                ((DeployModuleService) moduleDeployService).stopContainer(deployRequest.getId().toString());
            }

            if (deployRequest.getArtifacts() != null && !deployRequest.getArtifacts().isEmpty()) {
                for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                    deployOk = artifactDeployService.deploy(artifactRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(context.request());
                        return;
                    }
                }
            }

            if (deployRequest.getModules() != null && !deployRequest.getModules().isEmpty()) {
                for (DeployModuleRequest moduleRequest : deployRequest.getModules()) {
                    deployOk = moduleDeployService.deploy(moduleRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(context.request());
                        return;
                    }
                }
            }

            respondOk(context.request());
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
