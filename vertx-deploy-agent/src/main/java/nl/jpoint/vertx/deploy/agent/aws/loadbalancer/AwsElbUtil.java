package nl.jpoint.vertx.deploy.agent.aws.loadbalancer;

import com.amazonaws.util.EC2MetadataUtils;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.deploy.agent.aws.AwsInstance;
import nl.jpoint.vertx.deploy.agent.aws.AwsState;
import rx.Observable;

public class AwsElbUtil {

    private final String instanceId;
    private final AwsAutoScalingUtil awsAsUtil;
    private final AwsElbUtilV1 awsElbUtilV1;
    private final AwsElbUtilV2 awsElbUtilV2;


    public AwsElbUtil(DeployConfig config) {
        this.awsAsUtil = new AwsAutoScalingUtil(config);
        this.instanceId = EC2MetadataUtils.getInstanceId();
        this.awsElbUtilV1 = new AwsElbUtilV1(config);
        this.awsElbUtilV2 = new AwsElbUtilV2(config);


    }

    public Observable<AwsInstance> registerInstance(String groupId) {
        return awsAsUtil.listLoadBalancers(groupId)
                .flatMap(awsElbUtilV1::registerInstanceWithLoadBalancer)
                .mergeWith(awsAsUtil.listTargetGroups(groupId)
                        .flatMap(awsElbUtilV2::registerInstanceWithTargetGroup));
    }

    public Observable<AwsInstance> deRegisterInstance(String groupId) {
        return awsAsUtil.listLoadBalancers(groupId)
                .flatMap(awsElbUtilV1::deRegisterInstanceFromLoadbalancer)
                .mergeWith(awsAsUtil.listTargetGroups(groupId)
                        .flatMap(awsElbUtilV2::deRegisterInstanceTargetGroup));
    }

    public Observable<AwsState> pollForInstanceState(AwsInstance instance) {
        switch (instance.getVersion()) {
            case V1:
                return awsElbUtilV1.pollForInstanceState(instance.getId());
            case V2:
                return awsElbUtilV2.pollForInstanceState(instance.getId());
            default:
                throw new IllegalStateException();
        }
    }

    public String getInstanceId() {
        return instanceId;
    }
}
