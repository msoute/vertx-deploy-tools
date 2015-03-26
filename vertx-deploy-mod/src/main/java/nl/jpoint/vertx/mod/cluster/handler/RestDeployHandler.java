package nl.jpoint.vertx.mod.cluster.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.service.AwsService;
import nl.jpoint.vertx.mod.cluster.service.DeployModuleService;
import nl.jpoint.vertx.mod.cluster.service.DeployService;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

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
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
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

                LOG.info("[{} - {}]: Received deploy request with {} config(s), {} module(s) and {} artifact(s) ", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.getConfigs().size(), deployRequest.getModules().size(), deployRequest.getArtifacts().size());

                boolean deployOk = false;


                if (deployRequest.withElb()) {
                    if (awsService.registerRequest(deployRequest)) {
                        respondContinue(request, deployRequest.getId().toString());
                        awsService.deRegisterInstance(deployRequest.getId().toString());
                    }   else {
                        respondFailed(request);
                    }
                    return;
                }

                if (deployRequest.withRestart()) {
                    ((DeployModuleService) moduleDeployService).stopContainer(deployRequest.getId().toString());
                }

                for (DeployConfigRequest configRequest : deployRequest.getConfigs()) {
                    deployOk = configDeployService.deploy(configRequest);
                    if (!deployOk) {
                        respondFailed(request);
                        return;
                    }
                }

                if (deployOk || deployRequest.getConfigs().isEmpty()) {
                    for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                        deployOk = artifactDeployService.deploy(artifactRequest);
                        if (!deployOk) {
                            respondFailed(request);
                            return;
                        }
                    }
                }

                if (deployOk || deployRequest.getArtifacts().size() == 0) {
                    for (DeployModuleRequest moduleRequest : deployRequest.getModules()) {
                        deployOk = moduleDeployService.deploy(moduleRequest);

                        if (!deployOk) {
                            respondFailed(request);
                            return;
                        }
                    }
                }

                respondOk(request);
            }
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
