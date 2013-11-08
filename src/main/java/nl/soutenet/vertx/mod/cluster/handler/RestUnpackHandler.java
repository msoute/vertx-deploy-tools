package nl.soutenet.vertx.mod.cluster.handler;

import nl.soutenet.vertx.mod.cluster.service.DeploySiteService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class RestUnpackHandler implements Handler<HttpServerRequest> {
    private final DeploySiteService unpackService;

    public RestUnpackHandler(DeploySiteService unpackService) {

        this.unpackService = unpackService;
    }

    @Override
    public void handle(HttpServerRequest event) {

    }
}
