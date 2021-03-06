package nl.jpoint.vertx.deploy.agent.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.command.RunConsoleCommand;
import nl.jpoint.vertx.deploy.agent.request.DeployConfigRequest;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import static rx.Observable.just;

public class DeployConfigService implements DeployService<DeployConfigRequest, Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployConfigService.class);

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
                .map(DeployConfigRequest::restart)
                .doOnCompleted(() -> LOG.info("[{} - {}]: Done extracting config {}.", deployRequest.getLogName(), deployRequest.getId(), deployRequest.getModuleId()));

    }

    @Override
    public DeployConfig getConfig() {
        return config;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public String getLogType() {
        return LogConstants.DEPLOY_CONFIG_REQUEST;
    }

    private Observable<DeployConfigRequest> runTestCommand(DeployConfigRequest deployConfigRequest) {
        if (deployConfigRequest.getTestCommand().isPresent()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(vertx, deployConfigRequest.getTestCommand().get());
            return consoleCommand.executeAsync(deployConfigRequest);
        }
        return just(deployConfigRequest);
    }

    private Observable<DeployConfigRequest> runRestartCommand(DeployConfigRequest deployConfigRequest) {
        if (deployConfigRequest.getRestartCommand().isPresent()) {
            RunConsoleCommand consoleCommand = new RunConsoleCommand(vertx, deployConfigRequest.getRestartCommand().get());
            return consoleCommand.executeAsync(deployConfigRequest);
        }
        return just(deployConfigRequest);
    }
}
