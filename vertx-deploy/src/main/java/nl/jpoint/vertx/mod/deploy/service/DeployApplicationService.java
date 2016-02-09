package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.command.RunApplication;
import nl.jpoint.vertx.mod.deploy.command.StopApplication;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Map;

import static rx.Observable.just;

public class DeployApplicationService implements DeployService<DeployApplicationRequest, DeployApplicationRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DeployApplicationService.class);
    private final DeployConfig config;
    private final Map<String, String> installedModules;
    private final Vertx vertx;

    public DeployApplicationService(DeployConfig config, Vertx vertx) {
        this.config = config;
        this.vertx = vertx;
        this.installedModules = new ProcessUtils(config).listInstalledAndRunningModules();
    }

    @Override
    public Observable<DeployApplicationRequest> deployAsync(DeployApplicationRequest deployApplicationRequest) {
        return resolveSnapShotVersion(deployApplicationRequest)
                .flatMap(this::checkInstalled)
                .flatMap(this::checkRunning)
                .flatMap(request -> {
                    if (deployApplicationRequest.isInstalled()) {
                        return this.startApplication(request);
                    } else {
                        return this.stopApplication(request)
                                .flatMap(this::startApplication)
                                .flatMap(this::registerApplication);
                    }
                });
    }

    private Observable<DeployApplicationRequest> checkInstalled(DeployApplicationRequest deployApplicationRequest) {
        if (!installedModules.containsKey(deployApplicationRequest.getMavenArtifactId())) {
            LOG.info("[{} - {}]: Module ({}) not installed.", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId());
            deployApplicationRequest.setInstalled(false);
        } else {
            String installedModuleVersion = installedModules.get(deployApplicationRequest.getMavenArtifactId());
            boolean sameVersion = installedModuleVersion.equals(deployApplicationRequest.getVersion());
            if (sameVersion) {
                LOG.info("[{} - {}]: Module ({}) already installed.", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId());
            }
            deployApplicationRequest.setInstalled(sameVersion);
        }
        return just(deployApplicationRequest);
    }

    private Observable<DeployApplicationRequest> checkRunning(DeployApplicationRequest deployApplicationRequest) {
        new ProcessUtils(config).checkModuleRunning(deployApplicationRequest);
        if (!deployApplicationRequest.isRunning()) {
            LOG.info("[{} - {}]: Module ({}) already stopped.", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId());
        }
        return just(deployApplicationRequest);
    }

    private Observable<DeployApplicationRequest> stopApplication(DeployApplicationRequest deployApplicationRequest) {
        if (deployApplicationRequest.isRunning()) {
            StopApplication stopApplicationCommand = new StopApplication(vertx, config, installedModules);
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
        installedModules.put(deployApplicationRequest.getMavenArtifactId(), deployApplicationRequest.getVersion());
        return just(deployApplicationRequest);
    }

    @Override
    public DeployConfig getConfig() {
        return config;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    public Observable<Boolean> stopContainer() {
        LOG.info("[{}]: Stopping all running modules",LogConstants.INVOKE_CONTAINER);
        LOG.error("{}", installedModules.size());
        return Observable.from(installedModules.entrySet())
                .flatMap(entry -> {
                    StopApplication stopApplication = new StopApplication(vertx, config, installedModules);
                    String[] mavenIds = entry.getKey().split(":", 2);
                    DeployApplicationRequest request = new DeployApplicationRequest(mavenIds[0], mavenIds[1], entry.getValue(), null, "jar");
                    request.setRunning(false);
                    request.setInstalled(false);
                    return stopApplication.executeAsync(request);
                })
                .toList()
                .flatMap(x -> Observable.just(true));
    }
}
