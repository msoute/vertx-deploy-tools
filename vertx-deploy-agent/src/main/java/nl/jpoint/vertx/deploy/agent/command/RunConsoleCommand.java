package nl.jpoint.vertx.deploy.agent.command;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.deploy.agent.request.DeployConfigRequest;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import nl.jpoint.vertx.deploy.agent.util.ObservableCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import static rx.Observable.just;

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
        return observableCommand.execute(new ProcessBuilder().command(command.split("\\s+")))
                .flatMap(x -> just(deployConfigRequest))
                .doOnCompleted(() -> LOG.info("[{} - {}]: Finished running console command '{}'.", LogConstants.CONSOLE_COMMAND, deployConfigRequest.getId(), command));
    }
}