package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsyncClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static rx.Observable.just;

public class AwsElbUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbUtil.class);
    private final AmazonElasticLoadBalancingClient elbClient;
    private final AmazonElasticLoadBalancingAsyncClient elbAsyncClient;

    public AwsElbUtil(AwsContext context) {
        this.elbClient = new AmazonElasticLoadBalancingClient(context.getCredentials());
        this.elbClient.setRegion(context.getAwsRegion());
        this.elbAsyncClient = new AmazonElasticLoadBalancingAsyncClient(context.getCredentials());
        this.elbAsyncClient.setRegion(context.getAwsRegion());
    }

    public Observable<String> registerInstanceWithLoadBalancer(String instanceId, String loadBalancer) throws AwsException {
        if (instanceId == null || loadBalancer == null) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, loadBalancer);
            throw new IllegalStateException();
        }
        try {
            return Observable.from(elbAsyncClient.registerInstancesWithLoadBalancerAsync(new RegisterInstancesWithLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(x -> Observable.just(loadBalancer));
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }

    }

    public Observable<String> deRegisterInstanceFromLoadbalancer(String instanceId, String loadBalancer) throws AwsException {

        if (instanceId == null || loadBalancer == null) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, loadBalancer);
            throw new IllegalStateException();
        }

        try {
            return Observable.from(elbAsyncClient.deregisterInstancesFromLoadBalancerAsync(new DeregisterInstancesFromLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(x -> Observable.just(loadBalancer));
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }
    }


    public Observable<AwsState> pollForInstanceState(final String instanceId, final String loadBalancer) throws AwsException {
        try {
            return Observable.from(elbAsyncClient.describeInstanceHealthAsync(new DescribeInstanceHealthRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId))))
                    .flatMap(result -> {
                        Optional<InstanceState> state = result.getInstanceStates().stream().filter(i -> i.getInstanceId().equals(instanceId)).findFirst();
                        return just(state.isPresent() ? AwsState.map(state.get().getState()) : AwsState.UNKNOWN);
                    });
        } catch (AmazonClientException e) {
            LOG.error("Error executing request {}.", e);
            throw new AwsException(e);
        }
    }

    public List<String> listLBInstanceMembers(String loadBalancer) throws AwsException {
        if (loadBalancer != null) {
            try {
                DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(new DescribeLoadBalancersRequest().withLoadBalancerNames(loadBalancer));
                return result.getLoadBalancerDescriptions().stream().flatMap(d -> d.getInstances().stream()).map(Instance::getInstanceId).collect(Collectors.toList());
            } catch (AmazonClientException e) {
                LOG.error("Error executing request 'listLBInstanceMembers' -> {}", e);
                throw new AwsException(e);
            }
        }
        return Collections.emptyList();
    }


}
