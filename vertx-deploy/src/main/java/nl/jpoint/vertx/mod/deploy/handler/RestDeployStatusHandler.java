package nl.jpoint.vertx.mod.deploy.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployState;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.util.ApplicationDeployState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.jpoint.vertx.mod.deploy.util.HttpUtils.*;

public class RestDeployStatusHandler implements Handler<RoutingContext> {
    private static final Logger LOG = LoggerFactory.getLogger(RestDeployStatusHandler.class);

    private final AwsService deployAwsService;
    private final DeployApplicationService deployApplicationService;

    public RestDeployStatusHandler(AwsService deployAwsService, DeployApplicationService deployApplicationService) {
        this.deployAwsService = deployAwsService;
        this.deployApplicationService = deployApplicationService;
    }

    @Override
    public void handle(final RoutingContext context) {
        DeployState state = deployAwsService != null ? deployAwsService.getDeployStatus(context.request().params().get("id")) : DeployState.SUCCESS;

        if (!deployApplicationService.getDeployedApplicationsFailed().isEmpty()) {
            LOG.error("Some services failed to start, failing build");
            state = DeployState.FAILED;
            if (deployAwsService != null) {
                deployAwsService.failAllRunningRequests();
            }
        }
        DeployState deployState = state != null ? state : DeployState.CONTINUE;
        switch (deployState) {
            case SUCCESS:
                respondOk(context.request(), createStatusObject());
                deployApplicationService.cleanup();
                break;
            case UNKNOWN:
            case FAILED:
                respondFailed(context.request(), createStatusObject());
                deployApplicationService.cleanup();
                break;
            default:
                respondContinue(context.request(), deployState);
        }

    }

    private JsonObject createStatusObject() {
        JsonObject result = new JsonObject();
        result.put(ApplicationDeployState.OK.name(), new JsonArray(deployApplicationService.getDeployedApplicationsSuccess()));
        result.put(ApplicationDeployState.ERROR.name(), new JsonObject(deployApplicationService.getDeployedApplicationsFailed()));
        return result;
    }
}