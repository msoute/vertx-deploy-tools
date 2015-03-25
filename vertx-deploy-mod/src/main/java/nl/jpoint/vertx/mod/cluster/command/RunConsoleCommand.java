package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.Constants;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunConsoleCommand implements Command<String> {

    private static final Logger LOG = LoggerFactory.getLogger(RunConsoleCommand.class);

    private final String deployId;

    public RunConsoleCommand(String deployId) {
        this.deployId = deployId;
    }

    @Override
    public JsonObject execute(String command) {
        Process consoleCommand;
        final JsonObject result = new JsonObject();
        result.putBoolean(Constants.COMMAND_STATUS, false);


        LOG.info("[{} - {}]: Running console command {}", LogConstants.CONSOLE_COMMAND, deployId, command);

        try {
            consoleCommand = Runtime.getRuntime().exec(command);
            consoleCommand.waitFor();
            int exitValue = consoleCommand.exitValue();
            BufferedReader output = new BufferedReader(new InputStreamReader(consoleCommand.getInputStream()));
            String outputLine;
            while ((outputLine = output.readLine()) != null) {
                LOG.info("[{} - {}]: {}", LogConstants.CONSOLE_COMMAND, deployId, outputLine);
            }

            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(consoleCommand.getErrorStream()));
                String errorLine;
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error("[{} - {}]: {}", LogConstants.CONSOLE_COMMAND, deployId, errorLine);
                }
            }
            result.putBoolean(Constants.COMMAND_STATUS, true);
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to run command {} with error {}", LogConstants.CONSOLE_COMMAND, deployId, command, e);
            return result;
        }

        return result;
    }
}