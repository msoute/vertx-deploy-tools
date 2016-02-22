package nl.jpoint.vertx.mod.deploy.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class HttpUtils {
    public static JsonArray toArray(List<String> list) {
        return new JsonArray(list);
    }

    public static JsonObject toArray(Map<String, Object> map) {
        return new JsonObject(map);
    }
}
