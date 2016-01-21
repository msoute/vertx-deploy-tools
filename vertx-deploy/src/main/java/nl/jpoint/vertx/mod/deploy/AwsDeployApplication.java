package nl.jpoint.vertx.mod.deploy;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import nl.jpoint.vertx.mod.deploy.handler.*;
import nl.jpoint.vertx.mod.deploy.handler.servicebus.DeployHandler;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import nl.jpoint.vertx.mod.deploy.service.DeployArtifactService;
import nl.jpoint.vertx.mod.deploy.service.DeployConfigService;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AwsDeployApplication extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(AwsDeployApplication.class);

    private boolean initiated = false;

    @Override
    public void start() {
        MDC.put("service", Constants.SERVICE_ID);
        DeployConfig deployconfig = DeployConfig.fromJsonObject(config());
        if (config() == null) {
            LOG.error("Unable to read config file");
            throw new IllegalStateException("Unable to read config file");
        }
        final DeployApplicationService deployApplicationService = new DeployApplicationService(deployconfig, getVertx().fileSystem());
        final DeployArtifactService deployArtifactService = new DeployArtifactService(getVertx(),deployconfig);
        final DeployConfigService deployConfigService = new DeployConfigService(getVertx(), deployconfig);

        AwsService awsService = null;

        if (deployconfig.isAwsEnabled()) {
            awsService = (new AwsService(getVertx(), deployconfig));
        }

        Router router = Router.router(getVertx());
        router.post("/deploy/deploy").handler(new RestDeployHandler(deployApplicationService, deployArtifactService, deployConfigService, awsService, deployconfig.getAuthToken()));
        router.post("/deploy/module*").handler(new RestDeployModuleHandler(deployApplicationService));
        router.post("/deploy/artifact*").handler(new RestDeployArtifactHandler(deployArtifactService));

        if (deployconfig.isAwsEnabled()) {
            vertx.eventBus().consumer("aws.service.deploy", new DeployHandler(awsService, deployApplicationService, deployArtifactService, deployConfigService));
            router.get("/deploy/status/:id").blockingHandler(new RestDeployAwsHandler(awsService));
        }

        router.get("/status").handler(event -> {
            if (initiated) {
                event.response().setStatusCode(HttpResponseStatus.FORBIDDEN.code());
            } else {
                event.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            }
            event.response().end();
            event.response().close();
        });

        HttpServer server = vertx.createHttpServer().requestHandler(router::accept);
        server.listen(config().getInteger("http.port", 6789));
        initiated = true;
        LOG.info("{}: Instantiated module.", LogConstants.CLUSTER_MANAGER);
    }


    @Override
    public void stop() throws Exception {
        LOG.warn("Stopping deploy application");
    }
}
