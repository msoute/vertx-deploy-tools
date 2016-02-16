package nl.jpoint.vertx.mod.deploy.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.util.ApplicationDeployState;

public class StatusUpdateHandler implements Handler<RoutingContext> {

    private final DeployApplicationService deployApplicationService;

    public StatusUpdateHandler(DeployApplicationService deployApplicationService) {

        this.deployApplicationService = deployApplicationService;
    }

    @Override
    public void handle(RoutingContext event) {
        String moduleId = event.request().params().get("id");
        if (moduleId != null && !moduleId.isEmpty()) {
            ApplicationDeployState status = ApplicationDeployState.map(event.request().getParam("status"));
            String message = event.request().getParam("message");
            deployApplicationService.addApplicationDeployResult(ApplicationDeployState.OK.equals(status), message, moduleId);
        }
        event.request().response().end();
    }
}
