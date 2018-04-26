package nl.jpoint.vertx.deploy.agent.command;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.request.DeployApplicationRequest;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import nl.jpoint.vertx.deploy.agent.util.ObservableCommand;
import nl.jpoint.vertx.deploy.agent.util.ProcessUtils;
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
    private final LocalDateTime waitTimeout = LocalDateTime.now().plusSeconds(10);
    private final DeployConfig config;
    private final ProcessUtils processUtils;
    private final Vertx rxVertx;
    private boolean killed = false;

    private String moduleIdToStop;

    public StopApplication(io.vertx.core.Vertx vertx, DeployConfig config) {
        this.config = config;
        this.processUtils = new ProcessUtils(config);
        this.rxVertx = new Vertx(vertx);
        this.timeout = LocalDateTime.now().plusMinutes(config.getAwsMaxRegistrationDuration());
    }

    @Override
    public Observable<DeployApplicationRequest> executeAsync(DeployApplicationRequest request) {
        LOG.info("[{} - {}]: Waiting for module {} to stop.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
        return this.stopApplication(request)
                .flatMap(this::doPoll)
                .flatMap(this::removeRunFile);
    }

    private Observable<DeployApplicationRequest> removeRunFile(DeployApplicationRequest deployApplicationRequest) {
        deployApplicationRequest.setRunning(false);
        return rxVertx.fileSystem().rxExists(config.getRunDir() + moduleIdToStop)
                .toObservable()
                .flatMap(exists -> {
                    if (exists) {
                        LOG.info("[{} - {}]: Removing runfile for application with applicationId '{}'.", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), moduleIdToStop);
                        return rxVertx.fileSystem().rxDelete(config.getRunDir() + moduleIdToStop)
                                .toObservable()
                                .flatMap(x -> just(deployApplicationRequest));
                    } else {
                        return just(deployApplicationRequest);
                    }
                });
    }

    private Observable<DeployApplicationRequest> stopApplication(DeployApplicationRequest request) {
        moduleIdToStop = request.getMavenArtifactId() + ":" + new ProcessUtils(config).getRunningVersion(request);
        LOG.info("[{} - {}]: Stopping application with applicationId '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), moduleIdToStop);
        ProcessBuilder processBuilder = new ProcessBuilder().command(Arrays.asList(config.getVertxHome().resolve("bin/vertx").toString(), "stop", moduleIdToStop));
        ObservableCommand<DeployApplicationRequest> observableCommand = new ObservableCommand<>(request, 0, rxVertx, true);
        return observableCommand.execute(processBuilder)
                .flatMap(exitCode -> handleExitCode(request, exitCode))
                .flatMap(x -> just(request))
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
                    }
                    if (LocalDateTime.now().isAfter(waitTimeout) && !killed) {
                        LOG.info("[{} - {}]: Application {} killed.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
                        killed = ProcessUtils.killService(request.getMavenArtifactId());
                        return doPoll(request);
                    } else {
                        LOG.trace("[{} - {}]: Application {} still running.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getMavenArtifactId());
                        return doPoll(request);
                    }
                        }
                )
                .doOnError(t -> LOG.info("[{} - {}]: Error while Waiting for  module '{}' with applicationId '{}' to stop -> '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), t));
    }
}