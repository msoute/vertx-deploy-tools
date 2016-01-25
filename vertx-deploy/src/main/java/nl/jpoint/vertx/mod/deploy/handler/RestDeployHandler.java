package nl.jpoint.vertx.mod.deploy.handler;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.service.DeployService;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RestDeployHandler implements Handler<RoutingContext> {

    private final DeployService<DeployApplicationRequest> moduleDeployService;
    private final DeployService<DeployArtifactRequest> artifactDeployService;
    private final DeployService<DeployConfigRequest> configDeployService;
    private final AwsService awsService;
    private final String authToken;

    private final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);

    public RestDeployHandler(final DeployService<DeployApplicationRequest> moduleDeployService,
                             final DeployService<DeployArtifactRequest> artifactDeployService,
                             final DeployService<DeployConfigRequest> configDeployService,
                             final AwsService awsService,
                             final String authToken) {
        this.moduleDeployService = moduleDeployService;
        this.artifactDeployService = artifactDeployService;
        this.configDeployService = configDeployService;
        this.awsService = awsService;
        this.authToken = authToken;
    }

    @Override
    public void handle(final RoutingContext context) {
        context.request().bodyHandler(buffer -> {
            ObjectReader reader = new ObjectMapper().readerFor(DeployRequest.class);

            DeployRequest deployRequest;
            if (!StringUtils.isNullOrEmpty(context.request().getHeader("authToken")) || !authToken.equals(context.request().getHeader("authToken"))) {
                LOG.error("{}: Invalid authToken in request.", LogConstants.DEPLOY_REQUEST);
                respondFailed(context.request(), "Invalid authToken in request.");
                return;
            }

            String eventBody = new String(buffer.getBytes());

            if (eventBody.isEmpty()) {
                LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_REQUEST);
                respondFailed(context.request(), "No postdata in request.");
                return;
            }
            LOG.debug("{}: received postdata -> {} ", LogConstants.DEPLOY_REQUEST, eventBody);
            try {
                deployRequest = reader.readValue(eventBody);
            } catch (IOException e) {
                LOG.error("{}: Error while reading post data -> {}.", LogConstants.DEPLOY_REQUEST, e.getMessage());
                respondFailed(context.request(), "Error wile reading post data -> " + e.getMessage());
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
                    respondFailed(context.request(),"");
                }
                return;
            }

            // Observable.from(deployRequest.getConfigs()).flatMap(deployConfigRequest -> configDeployService.deploy(deployConfigRequest))

            if (deployRequest.getConfigs() != null && !deployRequest.getConfigs().isEmpty()) {
                for (DeployConfigRequest configRequest : deployRequest.getConfigs()) {
                    deployOk = configDeployService.deploy(configRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(context.request(), "Error deploying configs.");
                        return;
                    }
                    if (!deployRequest.withRestart() && deployOk.getBoolean("configChanged.", false)) {
                        deployRequest.setRestart(true);
                    }
                }
            }

            if (deployRequest.withRestart()) {
                ((DeployApplicationService) moduleDeployService).stopContainer();
            }

            if (deployRequest.getArtifacts() != null && !deployRequest.getArtifacts().isEmpty()) {
                for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                    deployOk = artifactDeployService.deploy(artifactRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(context.request(), "Error deploying artifacts." );
                        return;
                    }
                }
            }

            if (deployRequest.getModules() != null && !deployRequest.getModules().isEmpty()) {
                for (DeployApplicationRequest moduleRequest : deployRequest.getModules()) {
                    deployOk = moduleDeployService.deploy(moduleRequest);
                    if (!deployOk.getBoolean("result")) {
                        respondFailed(context.request(), "Error deploying modules." );
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

    private void respondFailed(HttpServerRequest request, String message) {
        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        request.response().end(message);
    }
}
