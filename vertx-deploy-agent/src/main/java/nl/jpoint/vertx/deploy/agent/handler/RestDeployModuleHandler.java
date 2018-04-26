package nl.jpoint.vertx.mod.deploy.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.util.HttpUtils;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.jpoint.vertx.mod.deploy.util.HttpUtils.*;

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
                respondBadRequest(context.request());
                return;
            }

            LOG.info("[{} - {}]: Received deploy module {}", LogConstants.DEPLOY_REQUEST, deployRequest.getId().toString(), deployRequest.toString());

            service.deployAsync(deployRequest)
                    .doOnCompleted(() -> respondOk(context.request()))
                    .doOnError(t -> respondFailed(context.request()));
        });
    }

}
