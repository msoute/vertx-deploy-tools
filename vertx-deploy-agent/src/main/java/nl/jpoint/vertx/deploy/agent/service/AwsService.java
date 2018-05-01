package nl.jpoint.vertx.deploy.agent.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.aws.state.AwsAsDeRegisterInstance;
import nl.jpoint.vertx.deploy.agent.aws.state.AwsAsRegisterInstance;
import nl.jpoint.vertx.deploy.agent.aws.state.AwsElbDeRegisterInstance;
import nl.jpoint.vertx.deploy.agent.aws.state.AwsElbRegisterInstance;
import nl.jpoint.vertx.deploy.agent.request.DeployRequest;
import nl.jpoint.vertx.deploy.agent.request.DeployState;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
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

    public AwsService(Vertx vertx, DeployConfig config) {
        this.vertx = vertx;
        this.config = config;
    }

    public void registerRequest(DeployRequest deployRequest) {
        if (runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error(LogConstants.REQUEST_ALREADY_REGISTERED, LogConstants.AWS_ELB_REQUEST, deployRequest.getId());
            throw new IllegalStateException("Request already registered.");
        }
        runningRequests.put(deployRequest.getId().toString(), deployRequest);
    }

    public Observable<DeployRequest> autoScalingDeRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error(LogConstants.REQUEST_NOT_REGISTERED, LogConstants.AWS_ELB_REQUEST, deployRequest.getId());
            this.failBuild(deployRequest.getId().toString(), LogConstants.REQUEST_NOT_REGISTERED, null);
            throw new IllegalStateException();
        }
        updateAndGetRequest(DeployState.WAITING_FOR_AS_DEREGISTER, deployRequest.getId().toString());
        AwsAsDeRegisterInstance deRegisterFromAsGroup = new AwsAsDeRegisterInstance(vertx, config, config.getAwsMaxRegistrationDuration());
        return deRegisterFromAsGroup.executeAsync(deployRequest);
    }

    public Observable<DeployRequest> autoScalingRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error(LogConstants.REQUEST_NOT_REGISTERED, LogConstants.AWS_ELB_REQUEST, deployRequest.getId());
            this.failBuild(deployRequest.getId().toString(), LogConstants.REQUEST_NOT_REGISTERED, null);
            throw new IllegalStateException();
        }
        updateAndGetRequest(DeployState.WAITING_FOR_AS_REGISTER, deployRequest.getId().toString());
        AwsAsRegisterInstance register = new AwsAsRegisterInstance(vertx, config, config.getAwsMaxRegistrationDuration());
        return register.executeAsync(deployRequest);
    }

    public Observable<DeployRequest> loadBalancerRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error(LogConstants.REQUEST_NOT_REGISTERED, LogConstants.AWS_ELB_REQUEST, deployRequest.getId().toString());
            this.failBuild(deployRequest.getId().toString(), LogConstants.REQUEST_NOT_REGISTERED, null);
            throw new IllegalStateException();
        }
        updateAndGetRequest(DeployState.WAITING_FOR_ELB_REGISTER, deployRequest.getId().toString());
        AwsElbRegisterInstance register = new AwsElbRegisterInstance(vertx, deployRequest.getId().toString(), config,
                s -> runningRequests.containsKey(s) && (!DeployState.FAILED.equals(runningRequests.get(s).getState()) || !DeployState.SUCCESS.equals(runningRequests.get(s).getState())));
        return register.executeAsync(deployRequest);
    }

    public Observable<DeployRequest> loadBalancerDeRegisterInstance(DeployRequest deployRequest) {
        if (!runningRequests.containsKey(deployRequest.getId().toString())) {
            LOG.error(LogConstants.REQUEST_NOT_REGISTERED, LogConstants.AWS_ELB_REQUEST, deployRequest.getId().toString());
            this.failBuild(deployRequest.getId().toString(), LogConstants.REQUEST_NOT_REGISTERED, null);
            throw new IllegalStateException();
        }
        updateAndGetRequest(DeployState.WAITING_FOR_ELB_DEREGISTER, deployRequest.getId().toString());
        AwsElbDeRegisterInstance register = new AwsElbDeRegisterInstance(vertx, config);
        return register.executeAsync(deployRequest);
    }


    public DeployRequest updateAndGetRequest(DeployState state, String buildId) {
        if (runningRequests.containsKey(buildId) && !DeployState.FAILED.equals(runningRequests.get(buildId).getState())) {
            LOG.info("[{} - {}]: Updating state to {}", LogConstants.AWS_ELB_REQUEST, buildId, state);
            runningRequests.get(buildId).setState(state);
            return runningRequests.get(buildId);
        }
        return null;
    }

    public void failBuild(String buildId, String reason, Throwable t) {
        LOG.error("[{} - {}]: Failing build.", LogConstants.AWS_ELB_REQUEST, buildId);
        if (runningRequests.containsKey(buildId)) {
            runningRequests.get(buildId).setState(DeployState.FAILED);
            runningRequests.get(buildId).setFailedReason(reason);
        }
    }

    public DeployRequest getDeployRequest(String deployId) {
        if (!runningRequests.containsKey(deployId)) {
            return null;
        }
        DeployRequest deployRequest = runningRequests.get(deployId);

        if (DeployState.SUCCESS.equals(deployRequest.getState()) || DeployState.FAILED.equals(deployRequest.getState())) {
            runningRequests.remove(deployId);
        }
        return runningRequests.get(deployId);
    }

    public void failAllRunningRequests() {
        runningRequests.forEach((id, r) -> r.setState(DeployState.FAILED));
    }

}
