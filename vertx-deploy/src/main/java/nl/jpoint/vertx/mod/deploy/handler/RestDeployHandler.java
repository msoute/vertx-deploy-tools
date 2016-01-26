package nl.jpoint.vertx.mod.deploy.handler;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.*;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import nl.jpoint.vertx.mod.deploy.service.DeployApplicationService;
import nl.jpoint.vertx.mod.deploy.service.DeployService;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.Optional;

public class RestDeployHandler implements Handler<RoutingContext> {

    private final DeployService<DeployApplicationRequest> moduleDeployService;
    private final DeployService<DeployArtifactRequest> artifactDeployService;
    private final DeployService<DeployConfigRequest> configDeployService;
    private final Optional<AwsService> awsService;
    private final String authToken;

    private final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);

    public RestDeployHandler(final DeployService<DeployApplicationRequest> moduleDeployService,
                             final DeployService<DeployArtifactRequest> artifactDeployService,
                             final DeployService<DeployConfigRequest> configDeployService,
                             final AwsService awsService,
                             final String authToken) {
        this.moduleDeployService = moduleDeployService;
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

            Observable.just(deployRequest)
                    .flatMap(this::registerRequest)
                    .flatMap(r -> respondContinue(r, context.request()))
                    .flatMap(this::deRegisterInstanceFromAutoScalingGroup)
                    .flatMap(this::deRegisterInstanceFromLoadBalancer)
                    .flatMap(this::doDeploy)
                    .flatMap(this::registerInstanceInAutoScalingGroup)
                    .flatMap(this::checkElbStatus)
                    .doOnCompleted(() -> this.respond(deployRequest, context.request()))
                    .doOnError(t -> this.respondFailed(deployRequest.getId().toString(), context.request(), t.getMessage()))
                    .subscribe();
            // Observable.from(deployRequest.getConfigs()).flatMap(deployConfigRequest -> configDeployService.deploy(deployConfigRequest))

        });
    }

    private Observable<DeployRequest> deRegisterInstanceFromLoadBalancer(DeployRequest deployRequest) {
        if (deployRequest.withElb() && !deployRequest.withAutoScaling() && awsService.isPresent()) {
            return awsService.get().loadBalancerDeRegisterInstance(deployRequest);
        } else {
            return Observable.just(deployRequest);
        }
    }


    private Observable<DeployRequest> registerRequest(DeployRequest deployRequest) {
        if (deployRequest.withElb()) {
            awsService.ifPresent(service ->
                    service.registerRequest(deployRequest)
            );
        }
        return Observable.just(deployRequest);
    }

    private Observable<DeployRequest> respondContinue(DeployRequest deployRequest, HttpServerRequest request) {
        if (deployRequest.withElb()) {
            request.response().setStatusMessage(deployRequest.getId().toString());
            request.response().end(deployRequest.getId().toString());
        }
        return Observable.just(deployRequest);
    }

    private Observable<DeployRequest> deRegisterInstanceFromAutoScalingGroup(DeployRequest deployRequest) {

        if (deployRequest.withAutoScaling() && awsService.isPresent()) {
            return awsService.get().autoScalingDeRegisterInstance(deployRequest);
        } else {
            return Observable.just(deployRequest);
        }
    }

    private Observable<DeployRequest> registerInstanceInAutoScalingGroup(DeployRequest deployRequest) {
        if (deployRequest.withAutoScaling() && awsService.isPresent()) {
            return awsService.get().autoScalingRegisterInstance(deployRequest);
        } else {
            return Observable.just(deployRequest);
        }
    }

    private Observable<DeployRequest> checkElbStatus(DeployRequest deployRequest) {
        if (deployRequest.withElb() && awsService.isPresent()) {
            return awsService.get().loadBalancerRegisterInstance(deployRequest);
        } else {
            return Observable.just(deployRequest);
        }
    }


    private Observable<DeployRequest> doDeploy(DeployRequest deployRequest) {
        JsonObject deployOk = null;
        if (deployRequest.getConfigs() != null && !deployRequest.getConfigs().isEmpty()) {
            for (DeployConfigRequest configRequest : deployRequest.getConfigs()) {
                deployOk = configDeployService.deploy(configRequest);
                if (!deployOk.getBoolean("result")) {
                    throw new IllegalStateException("Error deploying configs.");
                }
                if (!deployRequest.withRestart() && deployOk.getBoolean("configChanged.", false)) {
                    deployRequest.setRestart(true);
                }
            }
        }

        if (deployRequest.withRestart()) {
            ((DeployApplicationService) moduleDeployService).stopContainer();
        }

        if (deployRequest.getArtifacts() != null && !deployRequest.getArtifacts().isEmpty()) {
            for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                deployOk = artifactDeployService.deploy(artifactRequest);
                if (!deployOk.getBoolean("result")) {
                    throw new IllegalStateException("Error deploying artifacts.");
                }
            }
        }

        if (deployRequest.getModules() != null && !deployRequest.getModules().isEmpty()) {
            for (DeployApplicationRequest moduleRequest : deployRequest.getModules()) {
                deployOk = moduleDeployService.deploy(moduleRequest);
                if (!deployOk.getBoolean("result")) {
                    throw new IllegalStateException("Error deploying modules.");
                }
            }
        }
        return Observable.just(deployRequest);
    }

    private void respond(DeployRequest deployRequest, HttpServerRequest request) {
        request.response().setStatusCode(HttpResponseStatus.OK.code());
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.SUCCESS, deployRequest.getId().toString()));
        if (!deployRequest.withElb() && !deployRequest.withAutoScaling()) {
            request.response().end();
        }
    }

    private void respondFailed(String id, HttpServerRequest request, String message) {
        request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        request.response().end(message);
        if (id != null) {
            awsService.ifPresent(aws -> aws.failBuild(id));
        }

    }
}
