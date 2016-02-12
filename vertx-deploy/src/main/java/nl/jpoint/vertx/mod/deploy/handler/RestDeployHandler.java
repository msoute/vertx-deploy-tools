package nl.jpoint.vertx.mod.deploy.handler;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployState;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.service.DeployArtifactService;
import nl.jpoint.vertx.mod.deploy.service.DeployConfigService;
import nl.jpoint.vertx.mod.deploy.util.HttpUtils;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.Optional;

import static rx.Observable.just;

public class RestDeployHandler implements Handler<RoutingContext> {

    private final DeployApplicationService applicationApplicationService;
    private final DeployArtifactService artifactDeployService;
    private final DeployConfigService configDeployService;
    private final Optional<AwsService> awsService;
    private final String authToken;

    private final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);

    public RestDeployHandler(final DeployApplicationService deployApplicationService,
                             final DeployArtifactService artifactDeployService,
                             final DeployConfigService configDeployService,
                             final AwsService awsService,
                             final String authToken) {
        this.applicationApplicationService = deployApplicationService;
        this.artifactDeployService = artifactDeployService;
        this.configDeployService = configDeployService;
        this.awsService = Optional.ofNullable(awsService);
        this.authToken = authToken;
    }

    @Override
    public void handle(final RoutingContext context) {

        context.request().bodyHandler(buffer -> {
            ObjectReader reader = new ObjectMapper().readerFor(DeployRequest.class);

            DeployRequest deployRequest;
            if (StringUtils.isNullOrEmpty(context.request().getHeader("authToken")) || !authToken.equals(context.request().getHeader("authToken"))) {
                LOG.error("{}: Invalid authToken in request.", LogConstants.DEPLOY_REQUEST);
                respondFailed(null, context.request(), "Invalid authToken in request.");
                return;
            }

            String eventBody = new String(buffer.getBytes());

            if (eventBody.isEmpty()) {
                LOG.error("{}: No postdata in request.", LogConstants.DEPLOY_REQUEST);
                respondFailed(null, context.request(), "No postdata in request.");
                return;
            }
            LOG.debug("{}: received postdata -> {} ", LogConstants.DEPLOY_REQUEST, eventBody);
            try {
                deployRequest = reader.readValue(eventBody);
            } catch (IOException e) {
                LOG.error("{}: Error while reading post data -> {}.", LogConstants.DEPLOY_REQUEST, e.getMessage());
                respondFailed(null, context.request(), "Error wile reading post data -> " + e.getMessage());
                return;
            }

            LOG.info("[{} - {}]: Received deploy request with {} config(s), {} module(s) and {} artifact(s) ", LogConstants.DEPLOY_REQUEST,
                    deployRequest.getId().toString(),
                    deployRequest.getConfigs() != null ? deployRequest.getConfigs().size() : 0,
                    deployRequest.getModules() != null ? deployRequest.getModules().size() : 0,
                    deployRequest.getArtifacts() != null ? deployRequest.getArtifacts().size() : 0);

            just(deployRequest)
                    .flatMap(this::registerRequest)
                    .flatMap(r -> respondContinue(r, context.request()))
                    .flatMap(this::cleanup)
                    .flatMap(this::deRegisterInstanceFromAutoScalingGroup)
                    .flatMap(this::deRegisterInstanceFromLoadBalancer)
                    .flatMap(this::deployConfigs)
                    .flatMap(this::deployArtifacts)
                    .flatMap(this::stopContainer)
                    .flatMap(this::deployApplications)
                    .flatMap(this::registerInstanceInAutoScalingGroup)
                    .flatMap(this::checkElbStatus)
                    .doOnCompleted(() -> this.respond(deployRequest, context.request()))
                    .doOnError(t -> this.respondFailed(deployRequest.getId().toString(), context.request(), t.getMessage()))
                    .subscribe();
        });
    }

    private Observable<DeployRequest> cleanup(DeployRequest deployRequest) {
        return applicationApplicationService.cleanup(deployRequest);
    }

    private Observable<DeployRequest> deRegisterInstanceFromLoadBalancer(DeployRequest deployRequest) {
        if (deployRequest.withElb() && !deployRequest.withAutoScaling() && awsService.isPresent()) {
            return awsService.get().loadBalancerDeRegisterInstance(deployRequest);
        } else {
            return just(deployRequest);
        }
    }


    private Observable<DeployRequest> registerRequest(DeployRequest deployRequest) {
        awsService.ifPresent(service ->
                service.registerRequest(deployRequest));
        return just(deployRequest);
    }

    private Observable<DeployRequest> respondContinue(DeployRequest deployRequest, HttpServerRequest request) {
        if (deployRequest.withElb()) {
            request.response().setStatusMessage(deployRequest.getId().toString());
            request.response().end(deployRequest.getId().toString());
        }
        return just(deployRequest);
    }

    private Observable<DeployRequest> deRegisterInstanceFromAutoScalingGroup(DeployRequest deployRequest) {
        if (deployRequest.withAutoScaling() && awsService.isPresent()) {
            return awsService.get().autoScalingDeRegisterInstance(deployRequest);
        } else {
            return just(deployRequest);
        }
    }

    private Observable<DeployRequest> deployConfigs(DeployRequest deployRequest) {
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.DEPLOYING_CONFIGS, deployRequest.getId().toString()));
        if (deployRequest.getConfigs() != null && !deployRequest.getConfigs().isEmpty()) {
            return Observable.from(deployRequest.getConfigs())
                    .flatMap(configDeployService::deployAsync)
                    .toList()
                    .flatMap(x -> {
                        LOG.info("[{} - {}]: Done extracting all config.", LogConstants.DEPLOY_CONFIG_REQUEST, deployRequest.getId());
                        return just(x);
                    })
                    .flatMap(list -> {
                        if (list.contains(Boolean.TRUE)) {
                            deployRequest.setRestart(true);
                        }
                        return just(deployRequest);
                    });
        } else {
            return just(deployRequest);
        }
    }

    private Observable<DeployRequest> deployArtifacts(DeployRequest deployRequest) {
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.DEPLOYING_ARTIFACTS, deployRequest.getId().toString()));
        if (deployRequest.getArtifacts() != null && !deployRequest.getArtifacts().isEmpty()) {
            return Observable.from(deployRequest.getArtifacts())
                    .flatMap(artifactDeployService::deployAsync)
                    .toList()
                    .flatMap(x -> {
                        LOG.info("[{} - {}]: Done extracting all artifacts.", LogConstants.DEPLOY_ARTIFACT_REQUEST, deployRequest.getId());
                        return just(x);
                    })
                    .flatMap(x -> just(deployRequest));
        } else {
            return just(deployRequest);
        }
    }

    private Observable<DeployRequest> stopContainer(DeployRequest deployRequest) {
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.STOPPING_CONTAINER, deployRequest.getId().toString()));
        if (deployRequest.withRestart()) {
            return applicationApplicationService.stopContainer()
                    .flatMap(x -> just(deployRequest));
        }
        return just(deployRequest);
    }

    private Observable<DeployRequest> deployApplications(DeployRequest deployRequest) {
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.DEPLOYING_APPLICATIONS, deployRequest.getId().toString()));
        if (deployRequest.getModules() != null && !deployRequest.getModules().isEmpty()) {
            return Observable.from(deployRequest.getModules())
                    .flatMap(applicationApplicationService::deployAsync)
                    .toList()
                    .flatMap(x -> just(deployRequest));
        }
        return just(deployRequest);
    }


    private Observable<DeployRequest> registerInstanceInAutoScalingGroup(DeployRequest deployRequest) {
        if (deployRequest.withAutoScaling() && awsService.isPresent()) {
            return awsService.get().autoScalingRegisterInstance(deployRequest);
        } else {
            return just(deployRequest);
        }
    }

    private Observable<DeployRequest> checkElbStatus(DeployRequest deployRequest) {
        if (deployRequest.withElb() && awsService.isPresent()) {
            return awsService.get().loadBalancerRegisterInstance(deployRequest);
        } else {
            return just(deployRequest);
        }
    }

    private void respond(DeployRequest deployRequest, HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.SUCCESS, deployRequest.getId().toString()));
        if (!deployRequest.withElb() && !deployRequest.withAutoScaling()) {
            JsonArray list = HttpUtils.toArray(applicationApplicationService.getDeployedApplications());
            if (list.isEmpty()) {
                request.response().end();
            } else {
                request.response().end(list.encode());
            }
        }
    }

    private void respondFailed(String id, HttpServerRequest request, String message) {
        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        if (message != null) {
            request.response().end(message);
        } else {
            request.response().end();
        }
        if (id != null) {
            awsService.ifPresent(aws -> aws.failBuild(id));
        }

    }
}
