package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import nl.jpoint.vertx.mod.deploy.util.ObservableCommand;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static rx.Observable.just;

public class RunApplication implements Command<DeployApplicationRequest> {

    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String INSTANCES = "INSTANCES";
    private static final String MAIN_SERVICE = "MAIN_SERVICE";
    private static final String CONFIG_FILE = "CONFIG_FILE";

    private static final Logger LOG = LoggerFactory.getLogger(RunApplication.class);
    private final Vertx rxVertx;
    private final DeployConfig deployConfig;

    public RunApplication(final io.vertx.core.Vertx vertx, final DeployConfig deployConfig) {
        this.rxVertx = new Vertx(vertx);
        this.deployConfig = deployConfig;
    }

    @Override
    public Observable<DeployApplicationRequest> executeAsync(final DeployApplicationRequest request) {
        LOG.info("[{} - {}]: Running module '{}'", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());
        return this.readServiceDefaults(request)
                .flatMap(this::startApplication)
                .doOnError(t -> LOG.error("[{} - {}]: Error running module '{}', {}", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId(), t.getMessage()));

    }

    Observable<DeployApplicationRequest> readServiceDefaults(DeployApplicationRequest request) {
        Properties serviceProperties = new Properties();
        String path = deployConfig.getServiceConfigLocation() + request.getGroupId() + ":" + request.getArtifactId();
        return rxVertx.fileSystem().existsObservable(path)
                .filter(Boolean.TRUE::equals)
                .map(x -> path)
                .switchIfEmpty(rxVertx.fileSystem().existsObservable(path.replace(":", "~"))
                        .filter(Boolean.TRUE::equals)
                        .map(x -> path.replace(":", "~")))
                .flatMap(location ->
                        rxVertx.fileSystem()
                                .readFileObservable(location)
                                .flatMap(buffer -> {
                                    try {
                                        serviceProperties.load(new ByteArrayInputStream(buffer.toString().getBytes()));
                                    } catch (IOException e) {
                                        LOG.error("[{} - {}]: Failed to initialize properties for module  {} with error '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), e.getMessage(), e);
                                        return just(request);
                                    }
                                    request.withJavaOpts(serviceProperties.getProperty(JAVA_OPTS, ""));
                                    request.withConfigLocation(serviceProperties.getProperty(CONFIG_FILE, ""));
                                    request.withInstances(serviceProperties.getProperty(INSTANCES, "1"));
                                    request.withMainService(serviceProperties.getProperty(MAIN_SERVICE, ""));
                                    return just(request);
                                }));
    }

    private Observable<DeployApplicationRequest> startApplication(DeployApplicationRequest deployApplicationRequest) {

        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(deployConfig.getVertxHome().resolve("bin/vertx").toString(), "start", getMavenCommand(deployApplicationRequest), "-id", deployApplicationRequest.getModuleId()));
        if (deployConfig.isMavenRemote()) {
            command.add("-Dvertx.maven.remoteRepos=" + buildRemoteRepo());
            command.add("-Dvertx.maven.remoteSnapshotPolicy=" + deployConfig.getRemoteRepoPolicy());
        }
        String applicationConfig = deployApplicationRequest.getConfigLocation().isEmpty() ? deployConfig.getConfigLocation() : deployApplicationRequest.getConfigLocation();
        if (!applicationConfig.isEmpty()) {
            command.add("-conf");
            command.add(applicationConfig);
        }
        if (!deployApplicationRequest.getJavaOpts().isEmpty() || !deployConfig.getDefaultJavaOpts().isEmpty()) {
            command.add("--java-opts");
            command.add(deployApplicationRequest.getJavaOpts());
            command.add(deployConfig.getDefaultJavaOpts());
        }
        command.add("--instances");
        command.add(deployApplicationRequest.getInstances());

        if (deployConfig.asCluster()) {
            command.add("-cluster");
        }
        command.add("-Dvertxdeploy.port=" + deployConfig.getHttpPort());
        command.add("-Dvertxdeploy.scope.test=" + deployApplicationRequest.isTestScope());

        ProcessBuilder processBuilder = new ProcessBuilder().command(command);
        ObservableCommand<DeployApplicationRequest> observableCommand = new ObservableCommand<>(deployApplicationRequest, 0, rxVertx, false);

        return observableCommand.execute(processBuilder)
                .flatMap(exitCode -> handleExitCode(deployApplicationRequest, exitCode))
                .flatMap(x -> just(deployApplicationRequest))
                .doOnCompleted(() -> LOG.info("[{} - {}]: Started module '{}' with applicationID '{}'", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId(), deployApplicationRequest.getMavenArtifactId()))
                .doOnError(t -> LOG.error("[{} - {}]: Failed to initialize application {} with error '{}'", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId(), t));
    }

    String getMavenCommand(DeployApplicationRequest request){
        if(!StringUtils.isBlank(request.getMainService())){
            return String.format("maven:%s::%s", request.getModuleId(), request.getMainService());
        }
        return String.format("maven:%s", request.getModuleId());
    }

    private String buildRemoteRepo() {
        URI remoteRepo = deployConfig.getNexusUrl();
        if (remoteRepo != null && deployConfig.isHttpAuthentication()) {
            URIBuilder builder = new URIBuilder(remoteRepo);
            builder.setUserInfo(deployConfig.getHttpAuthUser() + ":" + deployConfig.getHttpAuthPassword());
            return builder.toString();
        }
        return deployConfig.getNexusUrl().toString();
    }
}

