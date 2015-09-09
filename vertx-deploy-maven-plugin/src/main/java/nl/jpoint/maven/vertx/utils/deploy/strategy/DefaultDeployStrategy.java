package nl.jpoint.maven.vertx.utils.deploy.strategy;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;

public class DefaultDeployStrategy implements DeployStrategy {
    @Override
    public boolean calculate(DeployConfiguration activeConfiguration, AutoScalingGroup autoScalingGroup, long inService, long healthy, long inStandby) {
        return inService == 0 || autoScalingGroup.getMinSize() < inService;
    }
}
