package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractAwsDeployMojo extends AbstractDeployMojo {
    @Parameter(required = true, property = "deploy.as.id")
    protected String autoScalingGroup;
    @Parameter(required = true, property = "deploy.as.strategy")
    protected String strategy;
    @Parameter(property = "deploy.as.properties")
    protected String properties;
    @Parameter(defaultValue = "1", property = "deploy.as.max")
    private Integer maxGroupSize;
    @Parameter(defaultValue = "0", property = "deploy.as.min")
    private Integer minGroupSize;
    @Parameter(defaultValue = "false", property = "deploy.as.elb")
    private boolean useElb;
    @Parameter(defaultValue = "true", property = "deploy.as.private")
    private boolean usePrivateIp;
    @Parameter(defaultValue = "false", property = "deploy.as.test")
    private boolean isTestScope;
    @Parameter(defaultValue = "true", property = "deploy.as.config")
    private boolean deployConfig;
    @Parameter(defaultValue = "true", property = "deploy.as.restart")
    private boolean doRestart;
    @Parameter(defaultValue = "true", property = "deploy.as.decrement")
    private boolean decrementCapacity;
    @Parameter(defaultValue = "true", property = "deploy.as.ignoreInStandby")
    private boolean ignoreInStandby;
    @Parameter(defaultValue = "false", property = "deploy.as.allowSnapshots")
    private boolean deploySnapshots;
    @Parameter(defaultValue = "false", property = "deploy.as.stickiness")
    private boolean enableStickiness;
    @Parameter(property = "deploy.auth.token")
    private String authToken;

    DeployConfiguration createConfiguration() {
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
                .withStickiness(useElb && enableStickiness)
                .withProjectVersion(projectVersionAsString());
    }
}

