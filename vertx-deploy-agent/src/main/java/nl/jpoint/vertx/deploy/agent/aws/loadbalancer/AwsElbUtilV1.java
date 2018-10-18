package nl.jpoint.vertx.deploy.agent.aws.loadbalancer;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsync;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsyncClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.aws.AwsException;
import nl.jpoint.vertx.deploy.agent.aws.AwsInstance;
import nl.jpoint.vertx.deploy.agent.aws.AwsState;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Optional;

import static rx.Observable.just;

class AwsElbUtilV1 {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbUtilV1.class);
    private final AmazonElasticLoadBalancingAsync elbAsyncClient;
    private final String instanceId;

    AwsElbUtilV1(DeployConfig config) {
        this.elbAsyncClient = AmazonElasticLoadBalancingAsyncClientBuilder.standard().withRegion(config.getAwsRegion()).build();
        this.instanceId = EC2MetadataUtils.getInstanceId();
    }

    Observable<AwsInstance> registerInstanceWithLoadBalancer(String loadBalancer) {
        if (instanceId == null || loadBalancer == null) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, loadBalancer);
            throw new IllegalStateException();
        }
        try {
            return Observable.from(elbAsyncClient.registerInstancesWithLoadBalancerAsync(new RegisterInstancesWithLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(x -> Observable.just(AwsInstance.forELB(loadBalancer))
                            .doOnError(t -> LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, t)));
        } catch (AmazonClientException e) {
            LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }

    }

    Observable<AwsInstance> deRegisterInstanceFromLoadbalancer(String loadBalancer) {

        if (instanceId == null || loadBalancer == null) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, loadBalancer);
            throw new IllegalStateException();
        }

        try {
            return Observable.from(elbAsyncClient.deregisterInstancesFromLoadBalancerAsync(new DeregisterInstancesFromLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(x -> Observable.just(AwsInstance.forELB(loadBalancer)));
        } catch (AmazonClientException e) {
            LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }
    }

    Observable<AwsState> pollForInstanceState(final String loadBalancer) {
        try {
            return Observable.from(elbAsyncClient.describeInstanceHealthAsync(new DescribeInstanceHealthRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(result -> {
                        Optional<InstanceState> state = result.getInstanceStates().stream().filter(i -> i.getInstanceId().equals(instanceId)).findFirst();
                        return just(state.map(instanceState -> AwsState.map(instanceState.getState())).orElse(AwsState.UNKNOWN));
                    });
        } catch (AmazonClientException e) {
            LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }
    }


}
