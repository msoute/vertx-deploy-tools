package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsync;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsyncClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Optional;

import static nl.jpoint.vertx.mod.deploy.util.LogConstants.ERROR_EXECUTING_REQUEST;
import static rx.Observable.just;

public class AwsElbUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbUtil.class);
    private final AmazonElasticLoadBalancingAsync elbAsyncClient;
    private final String instanceId;

    public AwsElbUtil(DeployConfig config) {
        this.elbAsyncClient = AmazonElasticLoadBalancingAsyncClientBuilder.standard().withRegion(config.getAwsRegion()).build();
        this.instanceId = EC2MetadataUtils.getInstanceId();
    }

    public Observable<String> registerInstanceWithLoadBalancer(String loadBalancer) {
        if (instanceId == null || loadBalancer == null) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, loadBalancer);
            throw new IllegalStateException();
        }
        try {
            return Observable.from(elbAsyncClient.registerInstancesWithLoadBalancerAsync(new RegisterInstancesWithLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(x -> Observable.just(loadBalancer))
                    .doOnError(t -> LOG.error(ERROR_EXECUTING_REQUEST, t));
        } catch (AmazonClientException e) {
            LOG.error(ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }

    }

    public Observable<String> deRegisterInstanceFromLoadbalancer(String loadBalancer) {

        if (instanceId == null || loadBalancer == null) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, loadBalancer);
            throw new IllegalStateException();
        }

        try {
            return Observable.from(elbAsyncClient.deregisterInstancesFromLoadBalancerAsync(new DeregisterInstancesFromLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(x -> Observable.just(loadBalancer));
        } catch (AmazonClientException e) {
            LOG.error(ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }
    }


    public Observable<AwsState> pollForInstanceState(final String loadBalancer) {
        try {
            return Observable.from(elbAsyncClient.describeInstanceHealthAsync(new DescribeInstanceHealthRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(result -> {
                        Optional<InstanceState> state = result.getInstanceStates().stream().filter(i -> i.getInstanceId().equals(instanceId)).findFirst();
                        return just(state.isPresent() ? AwsState.map(state.get().getState()) : AwsState.UNKNOWN);
                    });
        } catch (AmazonClientException e) {
            LOG.error(ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }
    }

    public String getInstanceId() {
        return instanceId;
    }
}
