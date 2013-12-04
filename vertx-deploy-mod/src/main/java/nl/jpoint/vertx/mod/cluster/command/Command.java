package nl.jpoint.vertx.mod.cluster.command;

import org.vertx.java.core.json.JsonObject;

public interface Command<T> {
    public JsonObject execute(T request);

}
