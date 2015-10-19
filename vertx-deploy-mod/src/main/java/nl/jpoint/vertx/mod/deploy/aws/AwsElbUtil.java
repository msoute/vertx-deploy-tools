package nl.jpoint.vertx.mod.deploy.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AwsElbUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbUtil.class);

    private final String loadBalancer;
    private final String instanceId;
    private final AmazonElasticLoadBalancingClient elbClient;

    public AwsElbUtil(AwsContext context, String loadBalancer, String instanceId) {
        this.loadBalancer = loadBalancer;
        this.instanceId = instanceId;
        this.elbClient = new AmazonElasticLoadBalancingClient(context.getCredentials());
        this.elbClient.setRegion(context.getAwsRegion());
    }

    public AwsElbUtil(AwsContext context) {
        this(context, null, null);
    }

    public List<String> listLBInstanceMembers() throws AwsException {
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

    public boolean registerInstanceWithLoadbalancer() throws AwsException {
        if (instanceId != null && loadBalancer != null) {
            try {
                elbClient.registerInstancesWithLoadBalancer(new RegisterInstancesWithLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId)));
                return true;
            } catch (AmazonClientException e) {
                LOG.error("Error executing request 'registerInstanceWithLoadbalancer' -> {}", e);
                throw new AwsException(e);
            }
        }
        return false;
    }

    public boolean deRegisterInstanceFromLoadbalancer() throws AwsException {
        if (instanceId != null && loadBalancer != null) {
            try {
                elbClient.deregisterInstancesFromLoadBalancer(new DeregisterInstancesFromLoadBalancerRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId)));
                return true;
            } catch (AmazonClientException e) {
                LOG.error("Error executing request 'deRegisterInstanceFromLoadbalancer' -> {}", e);
                throw new AwsException(e);
            }
        }
        return false;
    }

    public AwsState getInstanceState() throws AwsException {
        return this.getInstanceState(instanceId, loadBalancer);
    }

    public AwsState getInstanceState(final String instanceId, final String loadBalancer) throws AwsException {
        if (instanceId != null && loadBalancer != null) {
            try {
                DescribeInstanceHealthResult result = elbClient.describeInstanceHealth(new DescribeInstanceHealthRequest().withLoadBalancerName(loadBalancer).withInstances(new Instance().withInstanceId(instanceId)));
                LOG.info(result.toString());
                Optional<InstanceState> state = result.getInstanceStates().stream().filter(i -> i.getInstanceId().equals(instanceId)).findFirst();
                if (state.isPresent()) {
                    return AwsState.map(state.get().getState());
                }
            } catch (AmazonClientException e) {
                LOG.error("Error executing request 'getInstanceState' -> {}", e);
                throw new AwsException(e);
            }
        }
        return AwsState.UNKNOWN;
    }

    public String forInstanceId() {
        return instanceId;
    }

    public String forLoadbalancer() {
        return loadBalancer;
    }
}
