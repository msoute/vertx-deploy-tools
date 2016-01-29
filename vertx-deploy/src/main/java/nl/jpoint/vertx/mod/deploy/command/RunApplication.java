package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static rx.Observable.just;

public class RunApplication implements Command<DeployApplicationRequest> {

    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String INSTANCES = "INSTANCES";
    private static final Logger LOG = LoggerFactory.getLogger(RunApplication.class);
    private Vertx rxVertx;
    private DeployConfig config;

    public RunApplication(final io.vertx.core.Vertx vertx, final DeployConfig config) {
        this.rxVertx = new Vertx(vertx);
        this.config = config;
    }

    public Observable<DeployApplicationRequest> executeAsync(final DeployApplicationRequest request) {
        LOG.info("[{} - {}]: Running module '{}'", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());
        return this.readServiceDefaults(request)
                .flatMap(this::startApplication);
    }

    private Observable<DeployApplicationRequest> readServiceDefaults(DeployApplicationRequest request) {
        Properties serviceProperties = new Properties();
        String path = "/etc/default/" + request.getGroupId() + ":" + request.getArtifactId();
        return rxVertx.fileSystem().existsObservable(path)
                .flatMap(exists -> {
                    if (exists) {
                        return rxVertx.fileSystem().readFileObservable(path)
                                .flatMap(buffer -> {
                                    try {
                                        serviceProperties.load(new ByteArrayInputStream(buffer.toString().getBytes()));
                                    } catch (IOException e) {
                                        LOG.error("[{} - {}]: Failed to initialize properties for module  {} with error '{}'", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId(), e.getMessage());
                                    }
                                    request.withJavaOpts(serviceProperties.getProperty(JAVA_OPTS));
                                    request.withInstances(serviceProperties.getProperty(INSTANCES, "1"));
                                    return just(request);
                                });
                    } else {
                        return just(request);
                    }
                });
    }


    private Observable<DeployApplicationRequest> startApplication(DeployApplicationRequest deployApplicationRequest) {
        try {
            List<String> command = new ArrayList<>();
            command.addAll(Arrays.asList(config.getVertxHome().resolve("bin/vertx").toString(), "start", "maven:" + deployApplicationRequest.getModuleId(), "-id", deployApplicationRequest.getModuleId()));
            if (!config.isMavenLocal()) {
                command.add("-Dvertx.maven.remoteRepos=" + buildRemoteRepo());
                command.add("-Dvertx.maven.remoteSnapshotPolicy=" + config.getRemoteRepoPolicy());
            }
            if (!config.getConfigLocation().isEmpty()) {
                command.add("-conf");
                command.add(config.getConfigLocation());
            }
            if (!deployApplicationRequest.getJavaOpts().isEmpty() || !config.getDefaultJavaOpts().isEmpty()) {
                command.add("--java-opts");
                command.add(deployApplicationRequest.getJavaOpts());
                command.add(config.getDefaultJavaOpts());
            }
            command.add("--instances");
            command.add(deployApplicationRequest.getInstances());

            if (config.asCluster()) {
                command.add("-cluster");
            }

            final Process runProcess = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
            runProcess.waitFor(1, TimeUnit.MINUTES);

            int exitValue = runProcess.exitValue();
            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                String errorLine;
                LOG.info("[{} - {}]: {} - Error Starting module '{}'", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId());
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error(errorLine);
                }
                throw new IllegalStateException();
            }
            LOG.info("[{} - {}]: Started module '{}' with applicationID '{}'", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId(), deployApplicationRequest.getMavenArtifactId());
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to initialize module {} with error '{}'", LogConstants.DEPLOY_REQUEST, deployApplicationRequest.getId(), deployApplicationRequest.getModuleId());
            throw new IllegalStateException();
        }
        return just(deployApplicationRequest);
    }

    private String buildRemoteRepo() {
        URI remoteRepo = config.getNexusUrl();
        if (remoteRepo != null && config.isHttpAuthentication()) {
            URIBuilder builder = new URIBuilder(remoteRepo);
            builder.setUserInfo(config.getHttpAuthUser() + ":" + config.getHttpAuthPassword());
            return builder.toString();
        }
        return config.getNexusUrl().toString();
    }
}

