package nl.jpoint.vertx.mod.cluster.service;

import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import org.vertx.java.core.http.HttpServerRequest;

public interface DeployService {
    public void deploy(ModuleRequest deployRequest, HttpServerRequest httpRequest);
}
