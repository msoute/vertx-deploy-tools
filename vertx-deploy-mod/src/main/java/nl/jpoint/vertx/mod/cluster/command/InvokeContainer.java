package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class InvokeContainer implements Command<String> {

    private static final Logger LOG = LoggerFactory.getLogger(InvokeContainer.class);

    private final String deployId;

    public InvokeContainer(String deployId) {
        this.deployId = deployId;
    }

    @Override
    public JsonObject execute(String command) {
        Process killProcess;
        final JsonObject result = new JsonObject();
        result.putBoolean(Constants.STOP_STATUS, false);


        LOG.info("[{} - {}]: Invoking container {}", LogConstants.INVOKE_CONTAINER, deployId, command);

        try {
            killProcess = Runtime.getRuntime().exec(new String[]{"sudo", "/etc/init.d/vertx", command});
            killProcess.waitFor();
            int exitValue = killProcess.exitValue();
            BufferedReader output = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));
            String outputLine;
            while ((outputLine = output.readLine()) != null) {
                LOG.info("[{} - {}]: {}", LogConstants.INVOKE_CONTAINER, deployId, outputLine);
            }

            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(killProcess.getErrorStream()));
                String errorLine;
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error("[{} - {}]: {}", LogConstants.INVOKE_CONTAINER, deployId, errorLine);
                }
            }
            result.putBoolean(Constants.STOP_STATUS, true);
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to {} container", LogConstants.INVOKE_CONTAINER, deployId, command);
            return result;
        }

        return result;
    }
}
