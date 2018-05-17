package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import io.vertx.rxjava.core.file.FileSystem;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.command.RunApplication;
import nl.jpoint.vertx.mod.deploy.command.StopApplication;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static rx.Observable.just;

public class DeployApplicationService implements DeployService<DeployApplicationRequest, DeployApplicationRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployApplicationService.class);
    private final DeployConfig config;
    private final Vertx vertx;
    private final List<String> deployedApplicationsSuccess = new ArrayList<>();
    private final Map<String, Object> deployedApplicationsFailed = new HashMap<>();

    public DeployApplicationService(DeployConfig config, Vertx vertx) {
        this.config = config;
        this.vertx = vertx;
    }

    @Override
    public Observable<DeployApplicationRequest> deployAsync(DeployApplicationRequest deployApplicationRequest) {
        return resolveSnapShotVersion(deployApplicationRequest)
                .flatMap(this::checkModuleState)
                .flatMap(this::stopApplication)
                .flatMap(this::startApplication)
                .flatMap(this::registerApplication);
    }

    private Observable<DeployApplicationRequest> checkModuleState(DeployApplicationRequest deployApplicationRequest) {
        new ProcessUtils(config).checkModuleRunning(deployApplicationRequest);
        LOG.info("[{} - {}]: Module '{}' running : {}, sameVersion : {}.", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId(), deployApplicationRequest.isInstalled(), deployApplicationRequest.isInstalled());
        return just(deployApplicationRequest);
    }

    private Observable<DeployApplicationRequest> stopApplication(DeployApplicationRequest deployApplicationRequest) {
        if (deployApplicationRequest.isRunning() && !deployApplicationRequest.isInstalled()) {
            StopApplication stopApplicationCommand = new StopApplication(vertx, config);
            return stopApplicationCommand.executeAsync(deployApplicationRequest);
        } else {
            return just(deployApplicationRequest);
        }
    }

    private Observable<DeployApplicationRequest> startApplication(DeployApplicationRequest deployApplicationRequest) {
        if (!deployApplicationRequest.isRunning()) {
            RunApplication runModCommand = new RunApplication(vertx, config);
            return runModCommand.executeAsync(deployApplicationRequest);
        } else {
            return just(deployApplicationRequest);
        }
    }


    private Observable<DeployApplicationRequest> registerApplication(DeployApplicationRequest
                                                                             deployApplicationRequest) {
        io.vertx.rxjava.core.Vertx rxVertx = new io.vertx.rxjava.core.Vertx(vertx);
        return rxVertx.fileSystem()
                .existsObservable(config.getRunDir() + deployApplicationRequest.getModuleId())
                .flatMap(exists -> {
                    if (!exists) {
                        return rxVertx.fileSystem().createFileObservable(config.getRunDir() + deployApplicationRequest.getModuleId())
                                .flatMap(x -> just(deployApplicationRequest));
                    } else {
                        return just(deployApplicationRequest);
                    }
                });
    }

    @Override
    public DeployConfig getConfig() {
        return config;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    Observable<Boolean> stopContainer() {
        LOG.info("[{}]: Stopping all running modules", LogConstants.INVOKE_CONTAINER);
        return Observable.from(new ProcessUtils(config).listInstalledAndRunningModules().entrySet())
                .flatMap(entry -> {
                    StopApplication stopApplication = new StopApplication(vertx, config);
                    String[] mavenIds = entry.getKey().split(":", 2);
                    DeployApplicationRequest request = new DeployApplicationRequest(mavenIds[0], mavenIds[1], entry.getValue(), null, "jar");
                    request.setRunning(false);
                    request.setInstalled(false);
                    return stopApplication.executeAsync(request);
                })
                .toList()
                .flatMap(x -> {
                    LOG.info("[{}]: Done stopping all applications", LogConstants.INVOKE_CONTAINER);
                    return Observable.just(true);
                })
                .flatMap(x -> Observable.just(true));
    }

    Observable<DeployRequest> cleanup(DeployRequest deployRequest) {
        deployedApplicationsSuccess.clear();
        deployedApplicationsFailed.clear();
        return cleanup()
                .flatMap(x -> just(deployRequest));
    }

    public Observable<Boolean> cleanup() {
        List<String> runningApplications = new ProcessUtils(config).listModules();
        FileSystem fs = new io.vertx.rxjava.core.Vertx(vertx).fileSystem();


        return fs.readDirObservable(config.getRunDir())
                .flatMapIterable(x -> x)
                .flatMap(s -> just(Pattern.compile("/").splitAsStream(s).reduce((a, b) -> b).orElse("")))
                .filter(s -> !s.isEmpty() && !runningApplications.contains(s))
                .flatMap(file -> fs.deleteObservable(config.getRunDir() + file))
                .toList()
                .flatMap(x -> just(Boolean.TRUE).doOnError(t -> LOG.error("error")))
                .onErrorReturn(x -> {
                    LOG.error("Error during cleanup of run files {}", x.getMessage());
                    return Boolean.FALSE;
                });
    }

    public void addApplicationDeployResult(boolean succeeded, String message, String deploymentId) {
        if (succeeded && !deployedApplicationsSuccess.contains(deploymentId)) {
            deployedApplicationsSuccess.add(deploymentId);
        }
        if (!succeeded && !deployedApplicationsFailed.containsKey(deploymentId)) {
            deployedApplicationsFailed.put(deploymentId, message != null ? message : "No reason provided by application.");
        }

    }

    public List<String> getDeployedApplicationsSuccess() {
        return deployedApplicationsSuccess;
    }

    public Map<String, Object> getDeployedApplicationsFailed() {
        return deployedApplicationsFailed;
    }
}
