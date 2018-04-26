package nl.jpoint.vertx.deploy.agent;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import nl.jpoint.vertx.deploy.agent.handler.*;
import nl.jpoint.vertx.deploy.agent.service.*;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
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
        final DeployApplicationService deployApplicationService = new DeployApplicationService(deployconfig, getVertx());
        final DeployArtifactService deployArtifactService = new DeployArtifactService(getVertx(), deployconfig);
        final DeployConfigService deployConfigService = new DeployConfigService(getVertx(), deployconfig);
        final DefaultDeployService defaultDeployService = new DefaultDeployService(deployApplicationService, deployArtifactService, deployConfigService);

        this.createRunDir(deployconfig);

        deployApplicationService.cleanup().subscribe();
        AwsService awsService = null;
        AutoDiscoverDeployService autoDiscoverDeployService = null;

        if (deployconfig.isAwsEnabled()) {
            awsService = new AwsService(getVertx(), deployconfig);
            autoDiscoverDeployService = new AutoDiscoverDeployService(deployconfig, defaultDeployService, getVertx());
        }

        Router router = Router.router(getVertx());
        router.post("/deploy/deploy").handler(new RestDeployHandler(defaultDeployService, awsService, deployconfig.getAuthToken()));
        router.post("/deploy/module*").handler(new RestDeployModuleHandler(deployApplicationService));
        router.post("/deploy/artifact*").handler(new RestDeployArtifactHandler(deployArtifactService));
        router.get("/deploy/update*").handler(new StatusUpdateHandler(deployApplicationService));

        if (deployconfig.isAwsEnabled()) {
            router.get("/deploy/status/:id").handler(new RestDeployStatusHandler(awsService, deployApplicationService));
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

        server.listen(deployconfig.getHttpPort());
        initiated = true;
        LOG.info("{}: Instantiated module.", LogConstants.CLUSTER_MANAGER);

        if (deployconfig.isAwsEnabled() && deployconfig.isAwsAutoDiscover() && autoDiscoverDeployService != null) {
            autoDiscoverDeployService.autoDiscoverFirstDeploy();
        }
    }

    private void createRunDir(DeployConfig deployconfig) {
        if (!vertx.fileSystem().existsBlocking(deployconfig.getRunDir())) {
            LOG.debug("Creating runDir '{}'.", deployconfig.getRunDir());
            vertx.fileSystem().mkdirBlocking(deployconfig.getRunDir());
        }
    }

    @Override
    public void stop() throws Exception {
        LOG.warn("Stopping deploy application");
    }
}
