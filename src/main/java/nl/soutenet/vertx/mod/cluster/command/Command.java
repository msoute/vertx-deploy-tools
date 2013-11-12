package nl.soutenet.vertx.mod.cluster.command;


import nl.soutenet.vertx.mod.cluster.request.ModuleRequest;
import org.vertx.java.core.json.JsonObject;

public interface Command {
    public JsonObject execute(ModuleRequest request);

}
