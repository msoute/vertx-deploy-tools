package nl.jpoint.vertx.mod.deploy.command;

import org.vertx.java.core.json.JsonObject;

public interface Command<T> {
    JsonObject execute(T request);

}
