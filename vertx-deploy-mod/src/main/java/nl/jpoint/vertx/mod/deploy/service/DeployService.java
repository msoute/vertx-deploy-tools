package nl.jpoint.vertx.mod.deploy.service;

import org.vertx.java.core.json.JsonObject;

public interface DeployService<T> {
    JsonObject deploy(T deployRequest);

}
