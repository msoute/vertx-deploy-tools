package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.request.Request;
import nl.jpoint.maven.vertx.service.AutoScalingDeployService;
import nl.jpoint.maven.vertx.utils.DeployUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.List;

@Mojo(name = "deploy-single-as", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class VertxDeployAwsAsMojo extends AbstractDeployMojo {

    @Parameter(required = true, property = "deploy.as.id")
    private String autoScalingGroup;
    @Parameter(required = true, property = "deploy.as.strategy")
    private String strategy;
    @Parameter(required = false, defaultValue = "1", property = "deploy.as.max")
    private Integer maxGroupSize;
    @Parameter(required = false, defaultValue = "0", property = "deploy.as.min")
    private Integer minGroupSize;
    @Parameter(required = false, defaultValue = "false", property = "deploy.as.elb")
    private boolean useElb;
    @Parameter(required = false, defaultValue = "true", property = "deploy.as.private")
    private boolean usePrivateIp;
    @Parameter(required = false, defaultValue = "false", property = "deploy.as.test")
    private boolean isTestScope;
    @Parameter(required = false, defaultValue = "true", property = "deploy.as.config")
    private boolean deployConfig;
    @Parameter(required = false, defaultValue = "true", property = "deploy.as.restart")
    private boolean doRestart;
    @Parameter(required = false, defaultValue = "true", property = "deploy.as.decrement")
    private boolean decrementCapacity;
    @Parameter(required = false, defaultValue = "true", property = "deploy.as.ignoreInStandby")
    private boolean ignoreInStandby;
    @Parameter(required = false, defaultValue = "false", property = "deploy.as.allowSnapshots")
    private boolean deploySnapshots;
    @Parameter(required = false, defaultValue = "", property = "deploy.auth.token")
    private String authToken;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final DeployUtils utils = new DeployUtils(getLog(), project);
        activeConfiguration = this.createConfiguration();
        activeConfiguration.getExclusions().addAll(utils.parseExclusions(exclusions));
        final List<Request> deployModuleRequests = utils.createDeployApplicationList(activeConfiguration);
        final List<Request> deployArtifactRequests = utils.createDeployArtifactList(activeConfiguration);
        final List<Request> deployConfigRequests = utils.createDeployConfigList(activeConfiguration);

        getLog().info("Constructed deploy request with '" + deployConfigRequests.size() + "' configs, '" + deployArtifactRequests.size() + "' artifacts and '" + deployModuleRequests.size() + "' modules");
        getLog().info("Executing deploy request, waiting for Vert.x to respond.... (this might take some time)");

        AutoScalingDeployService service = new AutoScalingDeployService(activeConfiguration, region, port, requestTimeout, getServer(), getLog(), project.getProperties());
        service.deployWithAutoScaling(deployModuleRequests, deployArtifactRequests, deployConfigRequests);

    }

    private DeployConfiguration createConfiguration() {
        return new DeployConfiguration()
                .withAutoScalingGroup(autoScalingGroup)
                .withStrategy(strategy)
                .withMaxGroupSize(maxGroupSize)
                .withMinGroupSize(minGroupSize)
                .withElb(useElb)
                .withPrivateIp(usePrivateIp)
                .withTestScope(isTestScope)
                .withConfig(deployConfig)
                .withRestart(doRestart)
                .withDecrementCapacity(decrementCapacity)
                .withIgnoreInStandby(ignoreInStandby)
                .withDeploySnapshots(deploySnapshots)
                .withAuthToken(authToken)
                .withProjectVersion(projectVersionAsString());
    }
}
