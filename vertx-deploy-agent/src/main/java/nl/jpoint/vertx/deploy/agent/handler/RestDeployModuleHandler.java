package nl.jpoint.vertx.deploy.agent.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.deploy.agent.request.DeployApplicationRequest;
import nl.jpoint.vertx.deploy.agent.service.DeployApplicationService;
import nl.jpoint.vertx.deploy.agent.util.HttpUtils;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestDeployModuleHandler implements Handler<RoutingContext> {

    private static final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);
    private final DeployApplicationService service;

    public RestDeployModuleHandler(final DeployApplicationService service) {
        this.service = service;
    }

    @Override
    public void handle(final RoutingContext context) {
        context.request().bodyHandler(buffer -> {
            DeployApplicationRequest deployRequest = HttpUtils.readPostData(buffer, DeployApplicationRequest.class, LogConstants.DEPLOY_REQUEST);

            if (deployRequest == null) {
                HttpUtils.respondBadRequest(context.request());
                return;
            }

            LOG.info("[{} - {}]: Received deploy module {}", LogConstants.DEPLOY_REQUEST, deployRequest.getId(), deployRequest);

            service.deployAsync(deployRequest)
                    .doOnCompleted(() -> HttpUtils.respondOk(context.request()))
                    .doOnError(t -> HttpUtils.respondFailed(context.request()));
        });
    }

}
