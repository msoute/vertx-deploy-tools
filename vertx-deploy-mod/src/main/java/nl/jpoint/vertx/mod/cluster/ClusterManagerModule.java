package nl.jpoint.vertx.mod.cluster;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.handler.RestDeployArtifactHandler;
import nl.jpoint.vertx.mod.cluster.handler.RestDeployHandler;
import nl.jpoint.vertx.mod.cluster.service.DeployArtifactService;
import nl.jpoint.vertx.mod.cluster.service.DeployModuleService;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public class ClusterManagerModule extends Verticle {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterManagerModule.class);
    private DeployModuleService manager;

    @Override
    public void start() {

        DeployModuleService deployService = new DeployModuleService(getVertx(), container.config());
        DeployArtifactService deploySiteService = new DeployArtifactService(getVertx(), container.config());

        HttpServer httpServer = getVertx().createHttpServer();
        RouteMatcher matcher = new RouteMatcher();
        matcher.post("/deploy/module*", new RestDeployHandler(deployService));
        matcher.post("/deploy/site*", new RestDeployArtifactHandler(deploySiteService));
        matcher.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                LOG.error("{}: No match for request {}", LogConstants.CLUSTER_MANAGER, event.absoluteURI());
                event.response().setStatusCode(HttpResponseStatus.FORBIDDEN.code());
                event.response().end();
            }
        });

        httpServer.requestHandler(matcher);
        httpServer.listen(6789);
        LOG.info("{}: Instantiated module.", LogConstants.CLUSTER_MANAGER);

    }
}
