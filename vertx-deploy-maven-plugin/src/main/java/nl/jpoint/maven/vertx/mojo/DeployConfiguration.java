package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.utils.deploy.strategy.DeployStrategyType;
import org.apache.maven.model.Exclusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeployConfiguration {

    /**
     * The maven project version as String (groupId:artifactId:version)
     */
    private String projectVersion;
    /**
     * The configuration target id
     **/
    private String target;
    /**
     * List of hosts to deploy to
     **/
    private List<String> hosts = new ArrayList<>();
    /**
     * Enable / disable deploy of config objects
     **/
    private boolean deployConfig = true;
    /**
     * List of artifacts to exclude
     **/
    private List<Exclusion> exclusions = new ArrayList<>();
    /**
     * Deploy artifacts in test scope
     **/
    private boolean testScope = false;
    /**
     * restart all modules on host
     **/
    private boolean restart = false;

    /**
     * Allow deploy of snapshots
     **/
    private boolean deploySnapshots = false;

    /** AWS Generic  Properties **/
    /**
     * Use public / private AWS ip's
     **/
    private boolean awsPrivateIp = false;
    private boolean elb = false;
    private boolean stickiness = false;
    private boolean s3 = false;

    private List<String> stickyPorts = new ArrayList<>(Collections.singletonList("443"));
    /**
     * AWS AutoScaling Properties
     **/
    private String autoScalingGroupId;
    private boolean ignoreInStandby = false;
    private boolean decrementDesiredCapacity = true;
    private DeployStrategyType deployStrategy = DeployStrategyType.KEEP_CAPACITY;
    private Integer maxCapacity = -1;
    private Integer minCapacity = 1;
    private List<String> autoScalingProperties = new ArrayList<>();

    private String authToken;

    public String getAuthToken() {
        return authToken;
    }

    public String getAutoScalingGroupId() {
        return autoScalingGroupId;
    }

    public boolean isDeploySnapshots() {
        return deploySnapshots;
    }

    public void setDeploySnapshots(boolean deploySnapshots) {
        this.deploySnapshots = deploySnapshots;
    }

    public List<Exclusion> getExclusions() {
        return exclusions;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public boolean isTestScope() {
        return testScope;
    }

    void setTestScope(boolean testScope) {
        this.testScope = testScope;
    }

    public String getTarget() {
        return target;
    }

    public boolean useElbStatusCheck() {
        return this.elb;
    }

    public boolean isS3Enabled() {
        return this.s3;
    }

    public boolean doRestart() {
        return restart;
    }

    public boolean getAwsPrivateIp() {
        return this.awsPrivateIp;
    }

    public boolean isDeployConfig() {
        return deployConfig;
    }

    public void setWithConfig(Boolean withConfig) {
        this.deployConfig = withConfig;
    }

    public boolean isIgnoreInStandby() {
        return ignoreInStandby;
    }

    public boolean isDecrementDesiredCapacity() {
        return decrementDesiredCapacity;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public Integer getMinCapacity() {
        return minCapacity;
    }

    public DeployStrategyType getDeployStrategy() {
        return deployStrategy;
    }

    public void setDeployStrategy(DeployStrategyType deployStrategy) {
        this.deployStrategy = deployStrategy;
    }

    public boolean isSticky() {
        return stickiness;
    }

    public List<Integer> getStickyPorts() {
        return stickyPorts.stream().map(Integer::valueOf).collect(Collectors.toList());
    }

    public DeployConfiguration withStickyPorts(List<String> stickyPorts) {
        this.stickyPorts = stickyPorts;
        return this;
    }

    public DeployConfiguration withAutoScalingGroup(String autoScalingGroup) {
        this.autoScalingGroupId = autoScalingGroup;
        return this;
    }

    DeployConfiguration withStrategy(String strategy) {
        this.deployStrategy = DeployStrategyType.valueOf(strategy);
        return this;
    }

    DeployConfiguration withMaxGroupSize(Integer maxGroupSize) {
        this.maxCapacity = maxGroupSize;
        return this;
    }

    DeployConfiguration withMinGroupSize(Integer minGroupSize) {
        this.minCapacity = minGroupSize;
        return this;
    }

    DeployConfiguration withElb(boolean useElb) {
        this.elb = useElb;
        return this;
    }

    DeployConfiguration withS3(boolean useS3) {
        this.s3 = useS3;
        return this;
    }

    DeployConfiguration withPrivateIp(boolean usePrivateIp) {
        this.awsPrivateIp = usePrivateIp;
        return this;
    }

    DeployConfiguration withTestScope(boolean isTestScope) {
        this.testScope = isTestScope;
        return this;
    }

    DeployConfiguration withConfig(boolean deployConfig) {
        this.deployConfig = deployConfig;
        return this;
    }

    DeployConfiguration withRestart(boolean doRestart) {
        this.restart = doRestart;
        return this;
    }

    DeployConfiguration withDecrementCapacity(boolean decrementCapacity) {
        this.decrementDesiredCapacity = decrementCapacity;
        return this;
    }

    DeployConfiguration withIgnoreInStandby(boolean ignoreInStandby) {
        this.ignoreInStandby = ignoreInStandby;
        return this;
    }

    DeployConfiguration withDeploySnapshots(boolean deploySnapshots) {
        this.deploySnapshots = deploySnapshots;
        return this;
    }

    public DeployConfiguration withExclusions(List<Exclusion> exclusions) {
        this.exclusions = exclusions;
        return this;
    }

    DeployConfiguration withAuthToken(String authToken) {
        this.authToken = authToken;
        return this;
    }

    DeployConfiguration withStickiness(boolean stickiness) {
        this.stickiness = stickiness;
        return this;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    DeployConfiguration withProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
        return this;
    }

    public List<String> getAutoScalingProperties() {
        return autoScalingProperties;
    }
}
