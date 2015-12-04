package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.json.JsonObject;

public interface DeployService<T> {
    JsonObject deploy(T deployRequest);

}
