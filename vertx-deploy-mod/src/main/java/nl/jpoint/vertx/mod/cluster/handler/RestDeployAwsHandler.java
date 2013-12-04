package nl.jpoint.vertx.mod.cluster.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.request.DeployState;
import nl.jpoint.vertx.mod.cluster.service.AwsService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class RestDeployAwsHandler implements Handler<HttpServerRequest> {
    private final AwsService deployAwsService;

    public RestDeployAwsHandler(AwsService deployAwsService) {
        this.deployAwsService = deployAwsService;
    }

    @Override
    public void handle(final HttpServerRequest request) {
        DeployState state = deployAwsService.getDeployStatus(request.params().get("id"));

        switch (state) {
            case SUCCESS:
                respondOk(request);
                break;
            case UNKNOWN:
            case FAILED:
                respondFailed(request);
                break;
            default:
                respondContinue(request, state, request.params().get("id"));
        }

    }

    private void respondOk(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        request.response().end();
    }

    private void respondContinue(HttpServerRequest request, DeployState state, String id) {
        request.response().setStatusCode(HttpResponseStatus.ACCEPTED.code());

        request.response().setStatusMessage("Deploy in state : " + state.name());
        request.response().end();
    }

    private void respondFailed(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        request.response().setStatusMessage("Error");
        request.response().end();
    }
}