package nl.soutenet.vertx.mod.cluster;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.soutenet.vertx.mod.cluster.handler.RestDeployHandler;
import nl.soutenet.vertx.mod.cluster.handler.RestUnpackHandler;
import nl.soutenet.vertx.mod.cluster.service.ClusterDeployService;
import nl.soutenet.vertx.mod.cluster.service.DeploySiteService;
import nl.soutenet.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public class ClusterManagerModule extends Verticle {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterManagerModule.class);
    private ClusterDeployService manager;

    @Override
    public void start() {

        ClusterDeployService deployService = new ClusterDeployService(getVertx());
        DeploySiteService unpackService = new DeploySiteService();


        HttpServer httpServer = getVertx().createHttpServer();

        RouteMatcher matcher = new RouteMatcher();
        matcher.post("/deploy*", new RestDeployHandler(deployService));
        matcher.post("/unpack*", new RestUnpackHandler(unpackService));

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
