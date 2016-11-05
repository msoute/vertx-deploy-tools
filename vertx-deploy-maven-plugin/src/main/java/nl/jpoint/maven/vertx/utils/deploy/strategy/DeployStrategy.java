package nl.jpoint.maven.vertx.utils.deploy.strategy;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.utils.AwsState;
import nl.jpoint.maven.vertx.utils.Ec2Instance;

import java.util.List;

@FunctionalInterface
interface DeployStrategy {

    default boolean isDeployable(DeployConfiguration activeConfiguration, AutoScalingGroup autoScalingGroup, List<Ec2Instance> instances) {
        long healthyInstances = autoScalingGroup.getInstances().stream()
                .filter(i -> i.getLifecycleState().equals(LifecycleState.InService.toString()))
                .count();

        long inStandbyInstances = autoScalingGroup.getInstances().stream()
                .filter(i -> i.getLifecycleState().equals(LifecycleState.Standby.toString()))
                .count();

        long inServiceInstances = instances.stream().filter(i -> AwsState.INSERVICE.equals(i.getElbState())).count();
        return this.calculate(activeConfiguration, autoScalingGroup, inServiceInstances, healthyInstances, inStandbyInstances);
    }

    boolean calculate(DeployConfiguration activeConfiguration, AutoScalingGroup autoScalingGroup, long inService, long healthy, long inStandby);
}
