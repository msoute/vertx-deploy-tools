package nl.soutenet.vertx.mod.cluster.service;

import nl.soutenet.vertx.mod.cluster.request.ModuleRequest;
import org.vertx.java.core.http.HttpServerRequest;

public interface DeployService {
    public void deploy(ModuleRequest deployRequest, HttpServerRequest httpRequest);
}
