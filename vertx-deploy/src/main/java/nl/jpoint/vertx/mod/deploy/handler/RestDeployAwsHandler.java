package nl.jpoint.vertx.mod.deploy.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.request.DeployState;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import org.slf4j.MDC;

public class RestDeployAwsHandler implements Handler<RoutingContext> {
    private final AwsService deployAwsService;

    public RestDeployAwsHandler(AwsService deployAwsService) {
        this.deployAwsService = deployAwsService;
    }

    @Override
    public void handle(final RoutingContext context) {
        DeployState state = deployAwsService.getDeployStatus(context.request().params().get("id"));

        switch (state) {
            case SUCCESS:
                respondOk(context.request());
                break;
            case UNKNOWN:
            case FAILED:
                respondFailed(context.request());
                break;
            default:
                respondContinue(context.request(), state);
        }

    }

    private void respondOk(HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        request.response().end();
    }

    private void respondContinue(HttpServerRequest request, DeployState state) {
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