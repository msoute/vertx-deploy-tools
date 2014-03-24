package nl.jpoint.vertx.mod.cluster.handler.servicebus;

import nl.jpoint.vertx.mod.cluster.aws.AwsState;
import nl.jpoint.vertx.mod.cluster.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployModuleRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployRequest;
import nl.jpoint.vertx.mod.cluster.request.DeployState;
import nl.jpoint.vertx.mod.cluster.service.AwsService;
import nl.jpoint.vertx.mod.cluster.service.DeployArtifactService;
import nl.jpoint.vertx.mod.cluster.service.DeployModuleService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class DeployHandler implements Handler<Message<JsonObject>> {

    private final AwsService awsService;
    private final DeployModuleService deployModuleService;
    private final DeployArtifactService deployArtifactService;

    public DeployHandler(AwsService awsService, DeployModuleService deployModuleService, DeployArtifactService deployArtifactService) {
        this.awsService = awsService;
        this.deployModuleService = deployModuleService;
        this.deployArtifactService = deployArtifactService;
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
        switch (state) {
            case NOTREGISTERED:
            case OUTOFSERVICE :
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
        DeployRequest deployRequest =  awsService.updateAndGetRequest(DeployState.DEPLOYING_MODULES, deployId);

        boolean deployOk = false;

        for (DeployModuleRequest moduleRequest : deployRequest.getModules()) {
            deployOk = deployModuleService.deploy(moduleRequest);

            if (!deployOk) {
                awsService.failBuild(deployId);
                return;
            }
        }

        deployRequest =  awsService.updateAndGetRequest(DeployState.DEPLOYING_ARTIFACTS, deployId);

        if (deployOk) {
            for (DeployArtifactRequest artifactRequest : deployRequest.getArtifacts()) {
                deployOk = deployArtifactService.deploy(artifactRequest);

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
