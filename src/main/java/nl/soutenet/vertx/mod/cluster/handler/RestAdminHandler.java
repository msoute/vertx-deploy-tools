package nl.soutenet.vertx.mod.cluster.handler;

import nl.soutenet.vertx.mod.cluster.service.ClusterDeployService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class RestAdminHandler implements Handler<HttpServerRequest> {
    private final ClusterDeployService clusterDeployManager;

    public RestAdminHandler(final ClusterDeployService manager) {
        this.clusterDeployManager = manager;
    }

    @Override
    public void handle(HttpServerRequest event) {

    }
}
