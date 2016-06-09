package nl.jpoint.vertx.mod.deploy.command;

import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.util.ExitCodes;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import static rx.Observable.just;

public interface Command<T> {
    Logger LOG = LoggerFactory.getLogger(Command.class);

    Observable<T> executeAsync(T request);

    default Observable<Integer> handleExitCode(DeployApplicationRequest request, Integer exitCode) {
        if (exitCode != 0) {
            LOG.error("[{} -{}]: Error while initializing container {} with error {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), ExitCodes.values()[exitCode]);
            throw new IllegalStateException("Error while initializing container " + request.getModuleId() + " with error " + ExitCodes.values()[exitCode]);
        }
        return just(exitCode);
    }
}
