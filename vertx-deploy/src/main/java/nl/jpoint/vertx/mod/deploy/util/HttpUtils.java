package nl.jpoint.vertx.mod.deploy.util;

import io.vertx.core.json.JsonArray;

import java.util.List;

public class HttpUtils {
    public static JsonArray toArray(List<String> list) {
        JsonArray array = new JsonArray();
        list.stream().map(array::add);
        return array;
    }
}
