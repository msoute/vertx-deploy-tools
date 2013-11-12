package nl.soutenet.vertx.mod.cluster.service;

import nl.soutenet.vertx.mod.cluster.command.DownloadArtifact;
import nl.soutenet.vertx.mod.cluster.request.ModuleRequest;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class DeploySiteService implements DeployService {


    private final Vertx vertx;
    private final JsonObject config;

    public DeploySiteService(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }


    @Override
    public void deploy(ModuleRequest deployRequest, HttpServerRequest httpRequest) {
        DownloadArtifact command = new DownloadArtifact(config);
        command.execute(deployRequest);
    }
    public void resolveArtifact() {


    }


}
