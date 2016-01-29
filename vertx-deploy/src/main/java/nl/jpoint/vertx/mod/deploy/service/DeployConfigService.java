package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.command.RunConsoleCommand;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import rx.Observable;

import static rx.Observable.just;

public class DeployConfigService implements DeployService<DeployConfigRequest, Boolean> {

    private final Vertx vertx;
    private final DeployConfig config;

    public DeployConfigService(Vertx vertx, DeployConfig config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public Observable<Boolean> deployAsync(DeployConfigRequest deployRequest) {
        return resolveSnapShotVersion(deployRequest)
                .flatMap(this::downloadArtifact)
                .flatMap(this::parseArtifactContext)
                .flatMap(this::extractArtifact)
                .flatMap(this::runTestCommand)
                .flatMap(this::runRestartCommand)
                .map(DeployConfigRequest::restart);
    }

    @Override
    public DeployConfig getConfig() {
        return config;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    private Observable<DeployConfigRequest> runTestCommand(DeployConfigRequest deployConfigRequest) {
        if (deployConfigRequest.getTestCommand().isPresent()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(deployConfigRequest.getTestCommand().get());
            return consoleCommand.executeAsync(deployConfigRequest);
        }
        return just(deployConfigRequest);
    }

    private Observable<DeployConfigRequest> runRestartCommand(DeployConfigRequest deployConfigRequest) {
        if (deployConfigRequest.getRestartCommand().isPresent()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(deployConfigRequest.getRestartCommand().get());
            return consoleCommand.executeAsync(deployConfigRequest);
        }
        return just(deployConfigRequest);
    }
}
