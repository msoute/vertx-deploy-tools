package nl.jpoint.vertx.deploy.agent.aws.loadbalancer;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingAsync;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingAsyncClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.aws.AwsException;
import nl.jpoint.vertx.deploy.agent.aws.AwsInstance;
import nl.jpoint.vertx.deploy.agent.aws.AwsState;
import nl.jpoint.vertx.deploy.agent.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Objects;
import java.util.Optional;

import static rx.Observable.just;

class AwsElbUtilV2 {
    private static final Logger LOG = LoggerFactory.getLogger(AwsElbUtilV2.class);
    private final AmazonElasticLoadBalancingAsync elbAsyncClient;
    private final String instanceId;

    AwsElbUtilV2(DeployConfig config) {
        this.elbAsyncClient = AmazonElasticLoadBalancingAsyncClientBuilder.standard().withRegion(config.getAwsRegion()).build();
        this.instanceId = EC2MetadataUtils.getInstanceId();
    }


    Observable<AwsInstance> registerInstanceWithTargetGroup(String targetGroup) {
        if (Objects.isNull(instanceId) || Objects.isNull(targetGroup)) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, targetGroup);
            throw new IllegalStateException();
        }
        try {
            return Observable.from(elbAsyncClient.registerTargetsAsync(new RegisterTargetsRequest()
                    .withTargetGroupArn(targetGroup)
                    .withTargets(new TargetDescription()
                            .withAvailabilityZone("all")
                            .withId(instanceId))))
                    .flatMap(x -> Observable.just(AwsInstance.forALB(targetGroup)))
                    .doOnError(t -> LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, t));
        } catch (AmazonClientException e) {
            LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }

    }

    Observable<AwsInstance> deRegisterInstanceTargetGroup(String targetGroup) {

        if (Objects.isNull(instanceId) || Objects.isNull(targetGroup)) {
            LOG.error("Unable to register instance {}, on load balancer {}.", instanceId, targetGroup);
            throw new IllegalStateException();
        }

        try {
            return Observable.from(elbAsyncClient.deregisterTargetsAsync(new DeregisterTargetsRequest()
                    .withTargetGroupArn(targetGroup)
                    .withTargets(new TargetDescription()
                            .withId(instanceId))))
                    .flatMap(x -> Observable.just(AwsInstance.forALB(targetGroup)));
        } catch (AmazonClientException e) {
            LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }
    }


    Observable<AwsState> pollForInstanceState(final String targetGroup) {
        try {
            return Observable.from(elbAsyncClient.describeTargetHealthAsync(new DescribeTargetHealthRequest().withTargetGroupArn(targetGroup).withTargets(new TargetDescription().withId(instanceId))))
                    .flatMap(result -> {

                        Optional<TargetHealthDescription> targetState = result.getTargetHealthDescriptions().stream().filter(i -> i.getTarget().getId().equals(instanceId)).findFirst();
                        return just(targetState.map(instanceState -> AwsState.map(instanceState.getTargetHealth())).orElse(AwsState.UNKNOWN));
                    });
        } catch (AmazonClientException e) {
            LOG.error(LogConstants.ERROR_EXECUTING_REQUEST, e);
            throw new AwsException(e);
        }
    }


}
