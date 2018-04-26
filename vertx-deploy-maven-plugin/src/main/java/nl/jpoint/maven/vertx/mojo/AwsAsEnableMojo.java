package nl.jpoint.maven.vertx.mojo;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.utils.AwsAutoScalingDeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Deprecated
@Mojo(name = "as-enable")
public class AwsAsEnableMojo extends AbstractDeployMojo {

    @Parameter(required = true, property = "deploy.as.id")
    private String autoScalingGroupId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        DeployConfiguration deployConfiguration = new DeployConfiguration().withAutoScalingGroup(autoScalingGroupId);

        AwsAutoScalingDeployUtils asUtils = new AwsAutoScalingDeployUtils(region, deployConfiguration, getLog());

        AutoScalingGroup asGroup = asUtils.getAutoScalingGroup();
        if (asGroup.getInstances().isEmpty() && asGroup.getDesiredCapacity() == 0) {
            getLog().info("Adding 1 instance to auto scaling group with id " + autoScalingGroupId);
            asUtils.enableAsGroup(autoScalingGroupId);
        }
    }
}
