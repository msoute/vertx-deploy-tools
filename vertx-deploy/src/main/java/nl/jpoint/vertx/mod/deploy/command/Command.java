package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.core.json.JsonObject;

public interface Command<T> {
    JsonObject execute(T request);

}
