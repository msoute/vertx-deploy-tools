package nl.jpoint.maven.vertx.service.autoscaling;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.executor.WaitForInstanceRequestExecutor;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import nl.jpoint.maven.vertx.utils.Ec2Instance;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

public class SpinAndRemovePrePostHandler implements AutoScalingPrePostHandler {
    private final DeployConfiguration activeConfiguration;
    private final AwsAutoScalingDeployUtils awsDeployUtils;
    private final Log log;

    public SpinAndRemovePrePostHandler(DeployConfiguration activeConfiguration, AwsAutoScalingDeployUtils awsDeployUtils, Log log) {

        this.activeConfiguration = activeConfiguration;
        this.awsDeployUtils = awsDeployUtils;
        this.log = log;
    }

    public List<Ec2Instance> preDeploy(AutoScalingGroup asGroup) throws MojoFailureException, MojoExecutionException {
        List<Ec2Instance> instances = awsDeployUtils.getInstancesForAutoScalingGroup(log, asGroup);

        if (asGroup.getInstances().isEmpty()) {
            log.info("No instances found in autoscaling group, spinning new instance");
            WaitForInstanceRequestExecutor.InstanceStatus instanceStatus = newInstance -> awsDeployUtils.checkInstanceInService(newInstance.getInstanceId());
            awsDeployUtils.setDesiredCapacity(asGroup, asGroup.getDesiredCapacity() + 1);
            WaitForInstanceRequestExecutor waitForDeployedInstanceRequestExecutor = new WaitForInstanceRequestExecutor(log, 10);
            waitForDeployedInstanceRequestExecutor.executeRequest(asGroup, awsDeployUtils, instanceStatus);
            instances.addAll(awsDeployUtils.getInstancesForAutoScalingGroup(log, awsDeployUtils.getAutoScalingGroup()));
        }

        return instances;
    }

    public void postDeploy(AutoScalingGroup asGroup, Integer originalDesiredCapacity) {
        if (activeConfiguration.spindown()) {
            log.info("Setting desired capacity to : " + originalDesiredCapacity);
            awsDeployUtils.setDesiredCapacity(asGroup, originalDesiredCapacity);
        }
    }

    @Override
    public void handleError(AutoScalingGroup asGroup) {
        //Do nothing
    }
}
