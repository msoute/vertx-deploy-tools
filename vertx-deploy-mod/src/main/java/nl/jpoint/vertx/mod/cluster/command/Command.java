package nl.jpoint.vertx.mod.cluster.command;


import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import org.vertx.java.core.json.JsonObject;

public interface Command {
    public JsonObject execute(ModuleRequest request);

}
