package nl.jpoint.vertx.mod.deploy.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.service.DeployArtifactService;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RestDeployArtifactHandler implements Handler<RoutingContext> {

    private final DeployArtifactService service;
    private final Logger LOG = LoggerFactory.getLogger(RestDeployArtifactHandler.class);

    public RestDeployArtifactHandler(final DeployArtifactService service) {
        this.service = service;
    }

    @Override
    public void handle(final RoutingContext context) {

        context.request().bodyHandler(buffer -> {
            String postData = new String(buffer.getBytes());

            if (postData.isEmpty()) {
                LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_ARTIFACT_REQUEST);
                context.request().response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
                context.request().response().end();
                return;
            }

            DeployArtifactRequest artifactRequest;

            try {
                artifactRequest = new ObjectMapper().readerFor(DeployArtifactRequest.class).readValue(postData);
            } catch (IOException e) {
                LOG.error("[{}]: Failed to read postdata {}", postData);
                respondFailed(context.request());
                return;
            }

            LOG.info("[{} - {}]: Received deploy artifact request {}", LogConstants.DEPLOY_ARTIFACT_REQUEST, artifactRequest.getId().toString(), postData);

            service.deployAsync(artifactRequest)
                    .doOnCompleted(() -> respondOk(context.request()))
                    .doOnError(t -> respondFailed(context.request()));
        });
    }

    private void respondOk(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        request.response().end();
    }

    private void respondFailed(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        request.response().end();
    }


}
