package nl.jpoint.vertx.mod.deploy.command;

import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.util.ExitCodes;
import rx.Observable;

import static rx.Observable.just;

@FunctionalInterface
public interface Command<T> {
    Observable<T> executeAsync(T request);

    default Observable<Integer> handleExitCode(DeployApplicationRequest request, Integer exitCode) {
        if (exitCode != 0) {
            throw new IllegalStateException("Error while initializing container " + request.getModuleId() + " with error " + ExitCodes.values()[exitCode]);
        }
        return just(exitCode);
    }
}
