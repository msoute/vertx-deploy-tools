package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ObservableCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

public class RunConsoleCommand implements Command<DeployConfigRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(RunConsoleCommand.class);

    private final String command;
    private final Vertx rxVertx;

    public RunConsoleCommand(io.vertx.core.Vertx vertx, String command) {
        this.command = command;
        this.rxVertx = new Vertx(vertx);
    }

    @Override
    public Observable<DeployConfigRequest> executeAsync(DeployConfigRequest deployConfigRequest) {
        if (command == null || command.isEmpty()) {
            LOG.error("[{} - {}]: Failed to run empty command.", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId());
            throw new IllegalStateException();
        }

        ObservableCommand<DeployConfigRequest> observableCommand = new ObservableCommand<>(deployConfigRequest, 0, rxVertx);
        return observableCommand.execute(new ProcessBuilder().command(command))
                .doOnCompleted(() -> LOG.info("[{} - {}]: Finished running console command '{}'.", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), command))
                .doOnError(t -> LOG.error("[{} - {}]: Failed to run command {} with error {}", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), command, t));
    }
}