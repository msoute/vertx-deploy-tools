package nl.jpoint.maven.vertx.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jpoint.maven.vertx.request.DeployResult;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;

public final class LogUtil {

    private LogUtil() {
        //hide
    }

    public static void logDeployResult(Log log, String result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        DeployResult deployResult;
        try {
            deployResult = new ObjectMapper().readValue(result, DeployResult.class);
            log.info("List of successfully deployed applications");
            deployResult.getSuccess().forEach(log::info);
            if (!deployResult.getError().isEmpty()) {
                log.error("List of applications that failed to deploy");
                deployResult.getError().forEach((key, entry) -> log.error(key + " -> " + entry));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            log.warn("Unable to parse deploy result -> " + result);
        }
    }
}