package nl.jpoint.maven.vertx.service.autoscaling;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import nl.jpoint.maven.vertx.utils.Ec2Instance;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.List;

public interface AutoScalingPrePostHandler {
    List<Ec2Instance> preDeploy(AutoScalingGroup asGroup) throws MojoFailureException, MojoExecutionException;

    void postDeploy(AutoScalingGroup asGroup, Integer originalDesiredCapacity);

    void handleError(AutoScalingGroup asGroup);
}
