package nl.jpoint.vertx.mod.cluster;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.handler.RestDeployArtifactHandler;
import nl.jpoint.vertx.mod.cluster.handler.RestDeployAwsHandler;
import nl.jpoint.vertx.mod.cluster.handler.RestDeployHandler;
import nl.jpoint.vertx.mod.cluster.handler.RestDeployModuleHandler;
import nl.jpoint.vertx.mod.cluster.handler.servicebus.DeployHandler;
import nl.jpoint.vertx.mod.cluster.service.AwsService;
import nl.jpoint.vertx.mod.cluster.service.DeployArtifactService;
import nl.jpoint.vertx.mod.cluster.service.DeployModuleService;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public class ClusterManagerModule extends Verticle {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterManagerModule.class);

    @Override
    public void start() {
        MDC.put("service", "deploy");
        DeployModuleService deployModuleService = new DeployModuleService(getVertx(), container.config());
        DeployArtifactService deployArtifactService = new DeployArtifactService(getVertx(), container.config());
        AwsService awsService = new AwsService(getVertx(), container.config());

        vertx.eventBus().registerLocalHandler("aws.service.deploy", new DeployHandler(awsService, deployModuleService, deployArtifactService));

        HttpServer httpServer = getVertx().createHttpServer();
        RouteMatcher matcher = new RouteMatcher();

        matcher.post("/deploy/deploy*", new RestDeployHandler(deployModuleService, deployArtifactService, awsService));
        matcher.post("/deploy/module*", new RestDeployModuleHandler(deployModuleService));
        matcher.post("/deploy/artifact*", new RestDeployArtifactHandler(deployArtifactService));
        matcher.get("/deploy/status/:id", new RestDeployAwsHandler(awsService));

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
