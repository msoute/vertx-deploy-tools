package nl.jpoint.vertx.mod.deploy.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.deploy.service.DeployService;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;

public class RestDeployModuleHandler implements Handler<RoutingContext> {

    private final DeployService service;
    private final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);

    public RestDeployModuleHandler(final DeployService service) {
        this.service = service;
    }

    @Override
    public void handle(final RoutingContext context) {
        context.request().bodyHandler(buffer -> {
            String postData = new String(buffer.getBytes());

            if (postData.isEmpty()) {
                LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_REQUEST);
                context.request().response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
                context.request().response().end();
                return;
            }

            DeployModuleRequest deployRequest;
            try {
                deployRequest = new ObjectMapper().readerFor(DeployModuleRequest.class).readValue(postData);
            } catch (IOException e) {
                LOG.error("[{}]: Failed to read postdata {}", postData);
                respondFailed(context.request());
                return;
            }

            LOG.info("[{} - {}]: Received deploy module {}", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.toString());
            JsonObject result = service.deploy(deployRequest);

            if (!result.getBoolean("result")) {
                respondFailed(context.request());
                return;
            }
            respondOk(context.request());
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
