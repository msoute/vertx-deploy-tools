package nl.jpoint.vertx.mod.deploy.util;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class HttpUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    private HttpUtils() {
        // hide
    }

    public static JsonArray toArray(List<String> list) {
        return new JsonArray(list);
    }

    public static JsonObject toArray(Map<String, Object> map) {
        return new JsonObject(map);
    }

    public static <T> T readPostData(Buffer buffer, Class<T> clazz, String logType) {
        if (buffer == null || buffer.length() == 0) {
            LOG.error("[{}]: No postdata in request.", logType);
            return null;
        }

        LOG.debug("[{}]: received POST data -> {} .", logType, new String(buffer.getBytes()));

        try {
            return new ObjectMapper().readerFor(clazz).readValue(buffer.getBytes());
        } catch (IOException e) {
            LOG.error("[{}]: Error while reading POST data -> {}.", logType, e.getMessage(), e);
            return null;
        }
    }

    public static boolean hasCorrectAuthHeader(RoutingContext context, String authToken, String logType) {
        if (StringUtils.isNullOrEmpty(context.request().getHeader("authToken")) || !authToken.equals(context.request().getHeader("authToken"))) {
            LOG.error("{}: Invalid authToken in request.", logType);
            return false;
        }
        return true;
    }

    private static void respond(HttpServerResponse response, HttpResponseStatus code, JsonObject status) {
        response.setStatusCode(code.code());
        if (status != null) {
            response.end(status.encode());
        } else {
            response.end();
        }
    }

    public static void respondOk(HttpServerRequest request, JsonObject status) {
        respond(request.response(), HttpResponseStatus.OK, status);
    }

    public static void respondOk(HttpServerRequest request) {
        respondOk(request, null);
    }

    public static void respondFailed(HttpServerRequest request, JsonObject status) {
        respond(request.response(), HttpResponseStatus.INTERNAL_SERVER_ERROR, status);
    }

    public static void respondFailed(HttpServerRequest request) {
        respondFailed(request, null);
    }

    public static void respondBadRequest(HttpServerRequest request) {
        respond(request.response(), HttpResponseStatus.BAD_REQUEST, null);
    }

    public static void respondContinue(HttpServerRequest request, DeployState state) {
        request.response().setStatusCode(HttpResponseStatus.ACCEPTED.code());
        request.response().setStatusMessage("Deploy in state : " + state.name());
        request.response().end();
    }


}
