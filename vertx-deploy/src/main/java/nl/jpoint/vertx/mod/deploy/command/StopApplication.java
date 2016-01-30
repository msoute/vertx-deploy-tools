package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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
        return just(request)
                .flatMap(this::stopApplication)
                .flatMap(this::doPoll);

    }

    private Observable<DeployApplicationRequest> stopApplication(DeployApplicationRequest request) {
        LOG.info("[{} - {}]: Stopping application with applicationId '{}'.", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        Process killProcess;
        try {
            killProcess = Runtime.getRuntime().exec(new String[]{config.getVertxHome().resolve("bin/vertx").toString(), "stop", request.getMavenArtifactId()});
            killProcess.waitFor(1, TimeUnit.MINUTES);
            int exitValue = killProcess.exitValue();
            BufferedReader output = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));
            String outputLine;
            while ((outputLine = output.readLine()) != null && !outputLine.isEmpty()) {
                LOG.trace("[{} - {}]: {}", LogConstants.DEPLOY_REQUEST, request.getId(), outputLine);
            }

            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(killProcess.getErrorStream()));
                String errorLine;
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error("[{} - {}]: {}", LogConstants.DEPLOY_REQUEST, request.getId(), errorLine);
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to stop module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }
        return just(request);
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
