package nl.jpoint.vertx.mod.deploy.service;


import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static rx.Observable.just;

public class DefaultDeployService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDeployService.class);

    private final DeployApplicationService applicationDeployService;
    private final DeployArtifactService artifactDeployService;
    private final DeployConfigService configDeployService;

    public DefaultDeployService(final DeployApplicationService deployApplicationService,
                                final DeployArtifactService artifactDeployService,
                                final DeployConfigService configDeployService) {

        this.applicationDeployService = deployApplicationService;
        this.artifactDeployService = artifactDeployService;
        this.configDeployService = configDeployService;
    }

    public Observable<List<Boolean>> deployConfigs(UUID id, List<DeployConfigRequest> configs) {
        return Observable.from(configs)
                .flatMap(configDeployService::deployAsync)
                .toList()
                .flatMap(x -> {
                    LOG.info("[{} - {}]: Done extracting all config.", LogConstants.DEPLOY_CONFIG_REQUEST, id);
                    return just(x);
                });
    }

    public Observable<List<DeployArtifactRequest>> deployArtifacts(UUID id, List<DeployArtifactRequest> artifacts) {
        return Observable.from(artifacts)
                .flatMap(artifactDeployService::deployAsync)
                .toList()
                .flatMap(x -> {
                    LOG.info("[{} - {}]: Done extracting all artifacts.", LogConstants.DEPLOY_ARTIFACT_REQUEST, id);
                    return just(x);
                });

    }

    public Observable<List<DeployApplicationRequest>> deployApplications(UUID id, List<DeployApplicationRequest> applications) {
        return Observable.from(applications)
                .flatMap(applicationDeployService::deployAsync)
                .toList()
                .flatMap(x -> {
                    LOG.info("[{} - {}]: Done extracting all applications.", LogConstants.DEPLOY_REQUEST, id);
                    return just(x);
                });


    }

    public Observable<DeployRequest> cleanup(DeployRequest deployRequest) {
        return applicationDeployService.cleanup(deployRequest);
    }

    public Observable<Boolean> stopContainer() {
        return applicationDeployService.stopContainer();
    }

    public List<String> getDeployedApplicationsSuccess() {
        return applicationDeployService.getDeployedApplicationsSuccess();
    }

    public Map<String, Object> getDeployedApplicationsFailed() {
        return applicationDeployService.getDeployedApplicationsFailed();
    }
}
