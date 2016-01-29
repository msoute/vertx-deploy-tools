package nl.jpoint.vertx.mod.deploy.command;

import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunConsoleCommand implements Command<DeployConfigRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(RunConsoleCommand.class);

    private final String command;

    public RunConsoleCommand(String command) {
        this.command = command;
    }

    @Override
    public Observable<DeployConfigRequest> executeAsync(DeployConfigRequest deployConfigRequest) {
        Process consoleCommand;
        if (command == null || command.isEmpty()) {
            LOG.error("[{} - {}]: Failed to run empty command.", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId());
            throw new IllegalStateException();
        }

        LOG.info("[{} - {}]: Running console command '{}'", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), command);

        try {
            consoleCommand = Runtime.getRuntime().exec(command);
            consoleCommand.waitFor();
            int exitValue = consoleCommand.exitValue();
            BufferedReader output = new BufferedReader(new InputStreamReader(consoleCommand.getInputStream()));
            String outputLine;
            while ((outputLine = output.readLine()) != null) {
                LOG.info("[{} - {}]: {}", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), outputLine);
            }

            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(consoleCommand.getErrorStream()));
                String errorLine;
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error("[{} - {}]: {}", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), errorLine);
                }
            }
            LOG.info("[{} - {}]: result for  console command '{}' is {}", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), command, exitValue);
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to run command {} with error {}", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), command, e);
            throw new IllegalStateException();
        }

        return Observable.just(deployConfigRequest);
    }
}