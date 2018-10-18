package nl.jpoint.vertx.deploy.agent.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.deploy.agent.request.DeployArtifactRequest;
import nl.jpoint.vertx.deploy.agent.service.DeployArtifactService;
import nl.jpoint.vertx.deploy.agent.util.HttpUtils;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestDeployArtifactHandler implements Handler<RoutingContext> {

    private static final Logger LOG = LoggerFactory.getLogger(RestDeployArtifactHandler.class);
    private final DeployArtifactService service;

    public RestDeployArtifactHandler(final DeployArtifactService service) {
        this.service = service;
    }

    @Override
    public void handle(final RoutingContext context) {

        context.request().bodyHandler(buffer -> {
            String postData = new String(buffer.getBytes());

            DeployArtifactRequest artifactRequest = HttpUtils.readPostData(buffer, DeployArtifactRequest.class, LogConstants.DEPLOY_ARTIFACT_REQUEST);

            if (artifactRequest == null) {
                HttpUtils.respondBadRequest(context.request());
                return;
            }

            LOG.info("[{} - {}]: Received deploy artifact request {}", LogConstants.DEPLOY_ARTIFACT_REQUEST, artifactRequest.getId(), postData);

            service.deployAsync(artifactRequest)
                    .doOnCompleted(() -> HttpUtils.respondOk(context.request()))
                    .doOnError(t -> HttpUtils.respondFailed(context.request()));
        });
    }
}
