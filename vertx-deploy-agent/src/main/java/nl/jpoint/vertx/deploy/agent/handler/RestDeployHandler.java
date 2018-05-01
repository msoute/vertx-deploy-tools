package nl.jpoint.vertx.deploy.agent.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.deploy.agent.request.DeployRequest;
import nl.jpoint.vertx.deploy.agent.request.DeployState;
import nl.jpoint.vertx.deploy.agent.service.AwsService;
import nl.jpoint.vertx.deploy.agent.service.DefaultDeployService;
import nl.jpoint.vertx.deploy.agent.util.ApplicationDeployState;
import nl.jpoint.vertx.deploy.agent.util.HttpUtils;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Optional;

import static rx.Observable.just;

public class RestDeployHandler implements Handler<RoutingContext> {

    private static final Logger LOG = LoggerFactory.getLogger(RestDeployModuleHandler.class);
    private final DefaultDeployService deployService;
    private final Optional<AwsService> awsService;
    private final String authToken;

    public RestDeployHandler(final DefaultDeployService deployService,
                             final AwsService awsService,
                             final String authToken) {
        this.deployService = deployService;
        this.awsService = Optional.ofNullable(awsService);
        this.authToken = authToken;
    }

    @Override
    public void handle(final RoutingContext context) {

        context.request().bodyHandler(buffer -> {

            DeployRequest deployRequest = verifyIncomingRequest(context, buffer);

            if (deployRequest == null) {
                return;
            }

            deployRequest.setTimestamp(System.currentTimeMillis());

            LOG.info("[{} - {}]: Received deploy request with {} config(s), {} module(s) and {} artifact(s) ", LogConstants.DEPLOY_REQUEST,
                    deployRequest.getId().toString(),
                    deployRequest.getConfigs() != null ? deployRequest.getConfigs().size() : 0,
                    deployRequest.getModules() != null ? deployRequest.getModules().size() : 0,
                    deployRequest.getArtifacts() != null ? deployRequest.getArtifacts().size() : 0);

            executeDeploy(context, deployRequest);
        });
    }

    private DeployRequest verifyIncomingRequest(RoutingContext context, Buffer buffer) {
        if (!HttpUtils.hasCorrectAuthHeader(context, authToken, LogConstants.DEPLOY_REQUEST)) {
            respondFailed(null, context.request(), "Invalid authToken in request.", null);
            return null;
        }

        DeployRequest deployRequest = HttpUtils.readPostData(buffer, DeployRequest.class, LogConstants.DEPLOY_REQUEST);

        if (deployRequest == null) {
            respondFailed(null, context.request(), "Error wile reading post data ", null);
            return null;
        }

        if (deployRequest.withAutoScaling() && !awsService.isPresent()) {
            LOG.error("Asking for an Aws Enabled deploy. AWS is disabled");
            respondFailed(deployRequest.getId().toString(), context.request(), "Aws support disabled", null);
            return null;
        }

        return deployRequest;
    }

    private void executeDeploy(RoutingContext context, DeployRequest deployRequest) {
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
                .doOnError(t -> this.respondFailed(deployRequest.getId().toString(), context.request(), t.getMessage(), t))
                .subscribe();
    }


    private Observable<DeployRequest> cleanup(DeployRequest deployRequest) {
        return deployService.cleanup(deployRequest);
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
            return deployService.deployConfigs(deployRequest.getId(), deployRequest.getConfigs())
                    .doOnError(t -> {
                        LOG.error("[{} - {}]: Error while deploying config -> {}", LogConstants.DEPLOY_CONFIG_REQUEST, deployRequest.getId(), t.getMessage(), t);
                        awsService.ifPresent(aws -> aws.failBuild(deployRequest.getId().toString(), t.getMessage(), t));
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
            return deployService.deployArtifacts(deployRequest.getId(), deployRequest.getArtifacts())
                    .flatMap(x -> just(deployRequest));
        } else {
            return just(deployRequest);
        }
    }

    private Observable<DeployRequest> stopContainer(DeployRequest deployRequest) {
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.STOPPING_CONTAINER, deployRequest.getId().toString()));
        if (deployRequest.withRestart()) {
            return deployService.stopContainer()
                    .flatMap(x -> just(deployRequest));
        }
        return just(deployRequest);
    }

    private Observable<DeployRequest> deployApplications(DeployRequest deployRequest) {
        awsService.ifPresent(aws -> aws.updateAndGetRequest(DeployState.DEPLOYING_APPLICATIONS, deployRequest.getId().toString()));
        if (deployRequest.getModules() != null && !deployRequest.getModules().isEmpty()) {
            deployRequest.getModules().forEach(m -> m.withTestScope(deployRequest.isScopeTest()));
            return deployService.deployApplications(deployRequest.getId(), deployRequest.getModules())
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
        if (!request.response().ended() && !deployRequest.withElb() && !deployRequest.withAutoScaling()) {
            JsonObject result = new JsonObject();
            result.put(ApplicationDeployState.OK.name(), HttpUtils.toArray(deployService.getDeployedApplicationsSuccess()));
            result.put(ApplicationDeployState.ERROR.name(), HttpUtils.toArray(deployService.getDeployedApplicationsFailed()));
            if (result.isEmpty()) {
                request.response().end();
            } else {
                request.response().end(result.encodePrettily());
            }
        }
    }

    private void respondFailed(String id, HttpServerRequest request, String message, Throwable t) {

        if (!request.response().ended()) {

            request.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            JsonObject result = new JsonObject();
            result.put(ApplicationDeployState.OK.name(), HttpUtils.toArray(deployService.getDeployedApplicationsSuccess()));
            result.put(ApplicationDeployState.ERROR.name(), HttpUtils.toArray(deployService.getDeployedApplicationsFailed()));
            result.put("message", message);
            if (result.isEmpty()) {
                request.response().end();
            } else {
                request.response().end(result.encodePrettily());
            }
            if (id != null) {
                awsService.ifPresent(aws -> aws.failBuild(id, message, t));
            }
        }

    }
}
