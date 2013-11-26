package nl.jpoint.vertx.mod.cluster.handler;

import nl.jpoint.vertx.mod.cluster.service.DeployModuleService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

class RestAdminHandler implements Handler<HttpServerRequest> {
    private final DeployModuleService clusterDeployManager;

    public RestAdminHandler(final DeployModuleService manager) {
        this.clusterDeployManager = manager;
    }

    @Override
    public void handle(HttpServerRequest event) {

    }
}
