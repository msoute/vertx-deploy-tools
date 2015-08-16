package nl.jpoint.vertx.mod.deploy.handler.servicebus;

import nl.jpoint.vertx.mod.deploy.aws.AwsState;
import nl.jpoint.vertx.mod.deploy.request.*;
import nl.jpoint.vertx.mod.deploy.service.AwsService;
import nl.jpoint.vertx.mod.deploy.service.DeployArtifactService;
import nl.jpoint.vertx.mod.deploy.service.DeployConfigService;
import nl.jpoint.vertx.mod.deploy.service.DeployModuleService;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class DeployHandler implements Handler<Message<JsonObject>> {

    private static final Logger LOG = LoggerFactory.getLogger(DeployHandler.class);

    private final AwsService awsService;
    private final DeployModuleService deployModuleService;
    private final DeployArtifactService deployArtifactService;
    private final DeployConfigService deployConfigService;

    public DeployHandler(AwsService awsService, DeployModuleService deployModuleService, DeployArtifactService deployArtifactService, DeployConfigService deployConfigService) {
        this.awsService = awsService;
        this.deployModuleService = deployModuleService;
        this.deployArtifactService = deployArtifactService;
        this.deployConfigService = deployConfigService;
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String deployId = body.getString("id");
        AwsState state = AwsState.valueOf(body.getString("state"));

        if (!body.getBoolean("success")) {
            awsService.failBuild(deployId);
            return;
        }

        LOG.info("[{} - {}]: Handle internal deploy request with state {}", LogConstants.DEPLOY_REQUEST, deployId, state);
        switch (state) {
            case STANDBY:
            case NOTREGISTERED:
            case OUTOFSERVICE:
                this.deployArtifacts(deployId);
                break;
            case INSERVICE:
                this.registerInstance(deployId);
                break;
            default:
                // do nothing
        }
    }

    private void registerInstance(String deployId) {
        awsService.updateAndGetRequest(DeployState.SUCCESS, deployId);
    }

    private void deployArtifacts(String deployId) {
        DeployRequest deployRequest = awsService.updateAndGetRequest(DeployState.DEPLOYING_CONFIGS, deployId);

        boolean deployOk = false;

        if (deployRequest.withRestart()) {
            deployModuleService.stopContainer(deployId);
        }

        if (deployRequest.getConfigs() != null && !deployRequest.getConfigs().isEmpty()) {
            for (DeployConfigRequest configRequests : deployRequest.getConfigs()) {
                deployOk = deployConfigService.deploy(configRequests);
                if (!deployOk) {
                    awsService.failBuild(deployId);
                    return;
                }
            }
        }

        deployRequest = awsService.updateAndGetRequest(DeployState.DEPLOYING_ARTIFACTS, deployId);

        if (deployRequest.getArtifacts() != null && !deployRequest.getArtifacts().isEmpty()) {
            for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                deployOk = deployArtifactService.deploy(artifactRequest);

                if (!deployOk) {
                    awsService.failBuild(deployId);
                    return;
                }
            }
        }

        deployRequest = awsService.updateAndGetRequest(DeployState.DEPLOYING_MODULES, deployId);

        if (deployRequest.getModules() != null && !deployRequest.getModules().isEmpty()) {
            for (DeployModuleRequest moduleRequest : deployRequest.getModules()) {
                deployOk = deployModuleService.deploy(moduleRequest);

                if (!deployOk) {
                    awsService.failBuild(deployId);
                    return;
                }
            }
        }
        awsService.updateAndGetRequest(DeployState.WAITING_FOR_REGISTER, deployId);
        awsService.registerInstance(deployId);
    }


}
