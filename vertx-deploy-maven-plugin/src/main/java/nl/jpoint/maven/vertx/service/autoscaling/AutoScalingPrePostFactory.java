package nl.jpoint.maven.vertx.service.autoscaling;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import org.apache.maven.plugin.logging.Log;

public class AutoScalingPrePostFactory {

    private AutoScalingPrePostFactory() {
        // hides
    }

    public static AutoScalingPrePostHandler getPrePostHandler(DeployConfiguration activeConfiguration, AwsAutoScalingDeployUtils awsDeployUtils, Log log) {
        if (nl.jpoint.maven.vertx.utils.deploy.strategy.DeployStrategyType.SPIN_AND_REMOVE == activeConfiguration.getDeployStrategy()) {
            return new SpinAndRemovePrePostHandler(activeConfiguration, awsDeployUtils, log);
        } else {
            return new DefaultAutoScalingPrePostHandler(activeConfiguration, awsDeployUtils, log);
        }
    }

}
