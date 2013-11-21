package nl.jpoint.vertx.mod.cluster.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import nl.jpoint.vertx.mod.cluster.command.DownloadArtifact;
import nl.jpoint.vertx.mod.cluster.command.ExtractArtifact;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class DeployArtifactService implements DeployService {


    private final Vertx vertx;
    private final JsonObject config;

    public DeployArtifactService(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public void deploy(ModuleRequest deployRequest, HttpServerRequest httpRequest) {
        DownloadArtifact command = new DownloadArtifact(config);
        JsonObject downloadResult = command.execute(deployRequest);

        if (!downloadResult.getBoolean("success")) {
            httpRequest.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            httpRequest.response().end();
            return;
        }

        ExtractArtifact extractSite = new ExtractArtifact(vertx, config);
        JsonObject extractResult = extractSite.execute(deployRequest);

        if (!extractResult.getBoolean("success")) {
            httpRequest.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            httpRequest.response().end();
            return;
        }

        httpRequest.response().end();
    }
}
