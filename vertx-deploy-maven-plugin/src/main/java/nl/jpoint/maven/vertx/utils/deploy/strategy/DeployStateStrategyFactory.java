package nl.jpoint.maven.vertx.utils.deploy.strategy;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.utils.Ec2Instance;

import java.util.List;

public class DeployStateStrategyFactory {

    public static boolean isDeployable(DeployConfiguration activeConfiguration, AutoScalingGroup autoScalingGroup, List<Ec2Instance> instances) {
        boolean canDeploy = false;
        // default calculator
        switch (activeConfiguration.getDeployStrategy()) {
            case KEEP_CAPACITY:
                canDeploy = new KeepCapacityStrategy().isDeployable(activeConfiguration, autoScalingGroup, instances);
                break;
            case GUARANTEE_MINIMUM:
                canDeploy = new GuaranteeMinimumStrategy().isDeployable(activeConfiguration, autoScalingGroup, instances);
                break;
            case WHATEVER:
                canDeploy = new WhateverStrategy().isDeployable(activeConfiguration, autoScalingGroup, instances);
                break;
            default:

        }
        return canDeploy;
    }
}
