package nl.jpoint.vertx.mod.cluster.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.service.AwsService;
import nl.jpoint.vertx.mod.cluster.service.DeployService;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.IOException;

public class RestDeployHandler implements Handler<HttpServerRequest> {

    private final ObjectReader reader = new ObjectMapper().reader(DeployRequest.class);
    private final DeployService moduleDeployService;
    private final DeployService artifactDeployService;
    private final AwsService awsService;

    private final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);

    public RestDeployHandler(final DeployService moduleDeployService, final DeployService artifactDeployService, AwsService awsService) {

        this.moduleDeployService = moduleDeployService;
        this.artifactDeployService = artifactDeployService;
        this.awsService = awsService;
    }
    @Override
    public void handle(final HttpServerRequest request) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {

                DeployRequest deployRequest;

                if (event.getBytes() == null || event.getBytes().length == 0) {
                    LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_REQUEST);
                    respondFailed(request);
                    return;
                }
                try {
                    deployRequest = reader.readValue(event.getBytes());
                } catch (IOException e) {
                    LOG.error("{}: Error while reading postdata -> {}.", LogConstants.DEPLOY_REQUEST, e.getMessage());
                    respondFailed(request);
                    return;
                }

                LOG.info("[{} - {}]: Received deploy request with {} module(s) and {} artifact(s) ", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.getModules().size(), deployRequest.getArtifacts().size());

                boolean deployOk = false;


                if (deployRequest.withAws()) {
                    if (awsService.registerRequest(deployRequest)) {
                        respondContinue(request, deployRequest.getId().toString());
                        awsService.deRegisterInstance(deployRequest.getId().toString());
                    }   else {
                        respondFailed(request);
                    }
                    return;
                }

                for (DeployModuleRequest moduleRequest : deployRequest.getModules()) {
                    deployOk = moduleDeployService.deploy(moduleRequest);

                    if (!deployOk) {
                        respondFailed(request);
                        return;
                    }
                }

                if (deployOk) {
                    for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                        deployOk = artifactDeployService.deploy(artifactRequest);

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
