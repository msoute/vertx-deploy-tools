package nl.jpoint.vertx.deploy.agent.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.deploy.agent.request.DeployRequest;
import nl.jpoint.vertx.deploy.agent.request.DeployState;
import nl.jpoint.vertx.deploy.agent.service.AwsService;
import nl.jpoint.vertx.deploy.agent.service.DeployApplicationService;
import nl.jpoint.vertx.deploy.agent.util.ApplicationDeployState;
import nl.jpoint.vertx.deploy.agent.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        DeployRequest deployRequest = deployAwsService.getDeployRequest(context.request().params().get("id"));
        DeployState state = deployRequest != null ? deployRequest.getState() : DeployState.UNKNOWN;

        if (!deployApplicationService.getDeployedApplicationsFailed().isEmpty()) {
            LOG.error("Some services failed to start, failing build");
            state = DeployState.FAILED;
            deployAwsService.failAllRunningRequests();
        }

        DeployState deployState = state != null ? state : DeployState.CONTINUE;
        switch (deployState) {
            case SUCCESS:
                HttpUtils.respondOk(context.request(), createStatusObject(null));
                deployApplicationService.cleanup();
                break;
            case UNKNOWN:
            case FAILED:
                HttpUtils.respondFailed(context.request(), createStatusObject(deployRequest != null ? deployRequest.getFailedReason() : null));
                deployApplicationService.cleanup();
                break;
            default:
                HttpUtils.respondContinue(context.request(), deployState);
        }
    }

    private JsonObject createStatusObject(String reason) {
        JsonObject result = new JsonObject();
        result.put(ApplicationDeployState.OK.name(), HttpUtils.toArray(deployApplicationService.getDeployedApplicationsSuccess()));
        result.put(ApplicationDeployState.ERROR.name(), HttpUtils.toArray(deployApplicationService.getDeployedApplicationsFailed()));
        if (reason != null) {
            result.put("Reason", reason);
        }
        return result;
    }
}