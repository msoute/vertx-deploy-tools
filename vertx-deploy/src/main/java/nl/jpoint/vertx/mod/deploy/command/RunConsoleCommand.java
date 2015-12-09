package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        result.put(Constants.COMMAND_STATUS, false);
        if (command == null || command.isEmpty()) {
            LOG.error("[{} - {}]: Failed to run empty command.", LogConstants.CONSOLE_COMMAND, deployId);
            return result;
        }

        LOG.info("[{} - {}]: Running console command '{}'", LogConstants.CONSOLE_COMMAND, deployId, command);

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
            LOG.info("[{} - {}]: result for  console command '{}' is {}", LogConstants.CONSOLE_COMMAND, deployId, command, exitValue);
            result.put(Constants.COMMAND_STATUS, exitValue == 0);
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to run command {} with error {}", LogConstants.CONSOLE_COMMAND, deployId, command, e);
            return result;
        }

        return result;
    }
}