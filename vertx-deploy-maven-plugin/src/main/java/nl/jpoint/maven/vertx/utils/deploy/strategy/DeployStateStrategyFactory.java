package nl.jpoint.maven.vertx.utils.deploy.strategy;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.utils.Ec2Instance;

import java.util.List;

public final class DeployStateStrategyFactory {

    private DeployStateStrategyFactory() {
        // Hide
    }

    public static boolean isDeployable(DeployConfiguration activeConfiguration, AutoScalingGroup autoScalingGroup, List<Ec2Instance> instances) {
        boolean canDeploy = false;
        // default calculatorF
        switch (activeConfiguration.getDeployStrategy()) {
            case KEEP_CAPACITY:
                canDeploy = new KeepCapacityStrategy().isDeployable(activeConfiguration, autoScalingGroup, instances);
                break;
            case DEFAULT:
                canDeploy = new DefaultDeployStrategy().isDeployable(activeConfiguration, autoScalingGroup, instances);
                break;
            case GUARANTEE_MINIMUM:
                canDeploy = new GuaranteeMinimumStrategy().isDeployable(activeConfiguration, autoScalingGroup, instances);
                break;
            case WHATEVER:
                canDeploy = new WhateverStrategy().isDeployable(activeConfiguration, autoScalingGroup, instances);
                break;
            case SPIN_AND_REMOVE:
                canDeploy = true;
                break;
            default:

        }
        return canDeploy;
    }

    public static boolean isDeployableOnError(DeployConfiguration activeConfiguration, AutoScalingGroup asGroup, List<Ec2Instance> instances) {
        return activeConfiguration.getDeployStrategy().ordinal() > 1 && isDeployable(activeConfiguration, asGroup, instances);

    }
}
