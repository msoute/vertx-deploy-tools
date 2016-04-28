package nl.jpoint.vertx.mod.deploy.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.util.ApplicationDeployState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUpdateHandler implements Handler<RoutingContext> {
    private final Logger LOG = LoggerFactory.getLogger(StatusUpdateHandler.class);
    private final DeployApplicationService deployApplicationService;

    public StatusUpdateHandler(DeployApplicationService deployApplicationService) {
        this.deployApplicationService = deployApplicationService;
    }
    @Override
    public void handle(RoutingContext event) {
        String moduleId = event.request().getParam("id");
        LOG.debug("Received status request {}", event.request().uri());
        if (moduleId != null && !moduleId.isEmpty()) {
            ApplicationDeployState status = ApplicationDeployState.map(event.request().getParam("status"));
            String message = event.request().getParam("errormessage");
            LOG.trace("Adding result status : {} -> {} , message : {}, id: {} ", status, ApplicationDeployState.OK.equals(status), message, moduleId );
            deployApplicationService.addApplicationDeployResult(ApplicationDeployState.OK.equals(status), message, moduleId);
        }
        event.request().response().end();
    }
}
