package nl.jpoint.vertx.mod.cluster.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.cluster.service.DeployService;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.IOException;

public class RestDeployArtifactHandler implements Handler<HttpServerRequest> {

    private final DeployService service;
    private final Logger LOG = LoggerFactory.getLogger(RestDeployArtifactHandler.class);

    public RestDeployArtifactHandler(final DeployService service) {
        MDC.put("service", Constants.SERVICE_ID);
        this.service = service;
    }

    @Override
    public void handle(final HttpServerRequest request) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                byte[] postData = event.getBytes();

                if (postData == null || postData.length == 0) {
                    LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_SITE_REQUEST);
                    request.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
                    request.response().end();
                    return;
                }

                DeployArtifactRequest artifactRequest;

                try {
                    artifactRequest = new ObjectMapper().reader(DeployArtifactRequest.class).readValue(postData);
                } catch (IOException e) {
                    LOG.error("[{}]: Failed to read postdata {}", new String(postData));
                    respondFailed(request);
                    return;
                }


                LOG.info("[{} - {}]: Received deploy artifact request {}", LogConstants.DEPLOY_SITE_REQUEST, artifactRequest.getId().toString(), new String(postData));

                boolean result = service.deploy(artifactRequest);

                if (!result) {
                    respondFailed(request);
                    return;
                }
                respondOk(request);

            }
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