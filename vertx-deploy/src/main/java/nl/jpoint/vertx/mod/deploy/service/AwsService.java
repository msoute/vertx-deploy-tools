package nl.jpoint.vertx.mod.deploy.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.aws.AwsContext;
import nl.jpoint.vertx.mod.deploy.aws.state.AwsAsDeRegisterInstance;
import nl.jpoint.vertx.mod.deploy.aws.state.AwsAsRegisterInstance;
import nl.jpoint.vertx.mod.deploy.aws.state.AwsElbDeRegisterInstance;
import nl.jpoint.vertx.mod.deploy.aws.state.AwsElbRegisterInstance;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployState;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;

public class AwsService {
    private static final Logger LOG = LoggerFactory.getLogger(AwsService.class);
    private final Vertx vertx;
    private final DeployConfig config;
    private final Map<String, DeployRequest> runningRequests = new HashMap<>();
    private final AwsContext awsContext;

    public AwsService(Vertx vertx, DeployConfig config) {
        this.vertx = vertx;
        this.config = config;

        awsContext = AwsContext.build(config.getAwsAccessKey(), config.getAwsSecretAccessKey(), config.getAwsRegion());
    }

    public void registerRequest(DeployRequest deployRequest) {
        if (runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error("[{} - {}]: Request already registered.", LogConstants.AWS_ELB_REQUEST, deployRequest.getId());
            throw new IllegalStateException("Request already registered.");
        }
        runningRequests.put(deployRequest.getId().toString(), deployRequest);
    }

    public Observable<DeployRequest> autoScalingDeRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error("[{} - {}]: Request not registered.", LogConstants.AWS_ELB_REQUEST, deployRequest.getId().toString());
            this.failBuild(deployRequest.getId().toString());
            throw new IllegalStateException();
        }
        runningRequests.get(deployRequest.getId().toString()).setState(DeployState.WAITING_FOR_AS_DEREGISTER);
        AwsAsDeRegisterInstance deRegisterFromAsGroup = new AwsAsDeRegisterInstance(vertx, awsContext, config.getAwsMaxRegistrationDuration());
        return deRegisterFromAsGroup.executeAsync(deployRequest);
    }

    public Observable<DeployRequest> autoScalingRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error("[{} - {}]: Request not registered.", LogConstants.AWS_ELB_REQUEST, deployRequest.getId().toString());
            this.failBuild(deployRequest.getId().toString());
            throw new IllegalStateException();
        }
        runningRequests.get(deployRequest.getId().toString()).setState(DeployState.WAITING_FOR_AS_REGISTER);
        AwsAsRegisterInstance register = new AwsAsRegisterInstance(vertx, awsContext, config.getAwsMaxRegistrationDuration());
        return register.executeAsync(deployRequest);
    }

    public Observable<DeployRequest> loadBalancerRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error("[{} - {}]: Request not registered.", LogConstants.AWS_ELB_REQUEST, deployRequest.getId().toString());
            this.failBuild(deployRequest.getId().toString());
            throw new IllegalStateException();
        }
        runningRequests.get(deployRequest.getId().toString()).setState(DeployState.WAITING_FOR_ELB_REGISTER);
        AwsElbRegisterInstance register = new AwsElbRegisterInstance(vertx, awsContext, config.getAwsMaxRegistrationDuration());
        return register.executeAsync(deployRequest);
    }

    public Observable<DeployRequest> loadBalancerDeRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error("[{} - {}]: Request not registered.", LogConstants.AWS_ELB_REQUEST, deployRequest.getId().toString());
            this.failBuild(deployRequest.getId().toString());
            throw new IllegalStateException();
        }
        runningRequests.get(deployRequest.getId().toString()).setState(DeployState.WAITING_FOR_ELB_DEREGISTER);
        AwsElbDeRegisterInstance register = new AwsElbDeRegisterInstance(vertx, awsContext, config.getAwsMaxRegistrationDuration());
        return register.executeAsync(deployRequest);
    }


    public DeployRequest updateAndGetRequest(DeployState state, String buildId) {
        if (runningRequests.containsKey(buildId)) {
            LOG.info("[{} - {}]: Updating state to {}", LogConstants.AWS_ELB_REQUEST, buildId, state);
            runningRequests.get(buildId).setState(state);
            return runningRequests.get(buildId);
        }
        return null;
    }

    public void failBuild(String buildId) {
        LOG.error("[{} - {}]: Failing build.", LogConstants.AWS_ELB_REQUEST, buildId);
        if (runningRequests.containsKey(buildId)) {
            runningRequests.get(buildId).setState(DeployState.FAILED);
        }
    }

    public DeployState getDeployStatus(String deployId) {
        if (!runningRequests.containsKey(deployId)) {
            return DeployState.UNKNOWN;
        }

        DeployState state = runningRequests.get(deployId).getState();
        if (DeployState.SUCCESS.equals(state) || DeployState.FAILED.equals(state)) {
            runningRequests.remove(deployId);
        }
        return state;
    }
}
