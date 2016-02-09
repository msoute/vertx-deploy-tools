package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ObservableCommand;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.LocalDateTime;
import java.util.Arrays;

import static rx.Observable.just;

public class StopApplication implements Command<DeployApplicationRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(StopApplication.class);
    private static final Long POLLING_INTERVAL_IN_MS = 500L;
    private final LocalDateTime timeout;
    private final DeployConfig config;
    private final ProcessUtils processUtils;
    private final Vertx rxVertx;

    public StopApplication(io.vertx.core.Vertx vertx, DeployConfig config) {
        this.config = config;
        this.processUtils = new ProcessUtils(config);
        this.rxVertx = new Vertx(vertx);
        this.timeout = LocalDateTime.now().plusMinutes(1);
    }

    public Observable<DeployApplicationRequest> executeAsync(DeployApplicationRequest request) {
        LOG.info("[{} - {}]: Waiting for module {} to stop.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
        return this.stopApplication(request)
                .flatMap(this::doPoll);
    }

    private Observable<DeployApplicationRequest> stopApplication(DeployApplicationRequest request) {
        LOG.info("[{} - {}]: Stopping application with applicationId '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        ProcessBuilder processBuilder = new ProcessBuilder().command(Arrays.asList(new String[]{config.getVertxHome().resolve("bin/vertx").toString(), "stop", request.getModuleId()}));
        ObservableCommand<DeployApplicationRequest> observableCommand = new ObservableCommand<>(request, 0, rxVertx);
        return observableCommand.execute(processBuilder)
                .doOnError(t -> LOG.error("[{} - {}]: Failed to stop module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId()));
    }

    private Observable<DeployApplicationRequest> doPoll(DeployApplicationRequest request) {
        return rxVertx.timerStream(POLLING_INTERVAL_IN_MS).toObservable()
                .flatMap(x -> processUtils.checkModuleRunning(request))
                .flatMap(result -> {
                            if (LocalDateTime.now().isAfter(timeout)) {
                                LOG.error("[{} - {}]: Timeout while waiting for application to stop. ", LogConstants.DEPLOY_REQUEST, request.getId());
                                throw new IllegalStateException();
                            }
                            if (!request.isRunning()) {
                                LOG.info("[{} - {}]: Application {} stopped.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
                                return just(request);
                            } else {
                                LOG.info("[{} - {}]: Application {} still running.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
                                return doPoll(request);
                            }
                        }
                )
                .doOnError(t -> LOG.info("[{} - {}]: Error while Waiting for  module '{}' with applicationId '{}' to stop -> '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), t));
    }
}