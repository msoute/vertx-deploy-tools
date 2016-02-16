package nl.jpoint.vertx.mod.deploy.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class HttpUtils {
    public static JsonArray toArray(List<String> list) {
        JsonArray array = new JsonArray();
        list.stream()
                .map(moduleId -> new JsonObject().put(moduleId, "OK"))
                .map(array::add);
        return array;
    }

    public static JsonArray toArray(Map<String, String> map) {
        JsonArray array = new JsonArray();
        map.keySet().stream()
                .map(key -> new JsonObject().put(key, map.get(key)))
                .map(array::add);
        return array;
    }
}
