package nl.jpoint.vertx.mod.deploy.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.service.DeployArtifactService;
import nl.jpoint.vertx.mod.deploy.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.jpoint.vertx.mod.deploy.util.HttpUtils.*;
import static nl.jpoint.vertx.mod.deploy.util.LogConstants.DEPLOY_ARTIFACT_REQUEST;

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

            DeployArtifactRequest artifactRequest = HttpUtils.readPostData(buffer, DeployArtifactRequest.class, DEPLOY_ARTIFACT_REQUEST);

            if (artifactRequest == null) {
                respondBadRequest(context.request());
                return;
            }

            LOG.info("[{} - {}]: Received deploy artifact request {}", DEPLOY_ARTIFACT_REQUEST, artifactRequest.getId().toString(), postData);

            service.deployAsync(artifactRequest)
                    .doOnCompleted(() -> respondOk(context.request()))
                    .doOnError(t -> respondFailed(context.request()));
        });
    }
}
