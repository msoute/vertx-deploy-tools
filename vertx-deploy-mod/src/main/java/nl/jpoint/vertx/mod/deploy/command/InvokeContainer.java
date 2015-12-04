package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Stream;


public class InvokeContainer implements Command<String> {

    private static final Logger LOG = LoggerFactory.getLogger(InvokeContainer.class);

    private final String deployId;
    private String vertxHome;
    private String[] args;

    public InvokeContainer(String deployId, DeployConfig config) {
        this.deployId = deployId;
        this.vertxHome = config.getVertxHome();
    }

    @Override
    public JsonObject execute(String method) {
        Process killProcess;
        final JsonObject result = new JsonObject();
        result.put(Constants.STOP_STATUS, false);

        LOG.info("[{} - {}]: Invoking container {}", LogConstants.INVOKE_CONTAINER, deployId, method);

        try {
            String[] cmd = new String[]{vertxHome + "/bin/vertx", method};

            String[] command = Stream.concat(Arrays.stream(cmd), Arrays.stream(args))
                    .toArray(String[]::new);

            killProcess = Runtime.getRuntime().exec(command);
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
            result.put(Constants.STOP_STATUS, true);
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to {} container", LogConstants.INVOKE_CONTAINER, deployId, method);
            return result;
        }

        return result;
    }

    public InvokeContainer withArgs(String... args) {
        this.args = args;
        return this;
    }
}
