package nl.jpoint.vertx.mod.cluster.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.cluster.service.DeployService;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class RestDeployHandler implements Handler<HttpServerRequest> {

    private Logger LOG = LoggerFactory.getLogger(RestDeployHandler.class);

    private final DeployService service;

    public RestDeployHandler(final DeployService service) {
        this.service = service;
    }

    @Override
    public void handle(final HttpServerRequest request) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                byte[] postData = event.getBytes();

                if (postData == null || postData.length == 0) {
                    LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_REQUEST);
                    request.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
                    request.response().end();
                    return;
                }

                JsonObject jsonRequest = new JsonObject(new String(postData));
                DeployModuleRequest deployRequest = DeployModuleRequest.fromJsonMessage(jsonRequest);
                LOG.info("[{} - {}]: Received deploy request {}", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), jsonRequest.encode());
                service.deploy(deployRequest, request);
            }
        });
    }


}
