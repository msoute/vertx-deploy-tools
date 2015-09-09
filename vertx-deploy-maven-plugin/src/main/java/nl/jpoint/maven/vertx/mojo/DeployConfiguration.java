package nl.jpoint.maven.vertx.mojo;

import nl.jpoint.maven.vertx.utils.deploy.strategy.DeployStrategyType;
import org.apache.maven.model.Exclusion;

import java.util.ArrayList;
import java.util.List;

public class DeployConfiguration {
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
    private boolean useOpsWorks = false;
    private boolean useAutoScaling = false;
    private boolean elb = false;

    /**
     * AWS AutoScaling Properties
     **/
    private String autoScalingGroupId;
    private boolean ignoreInStandby = false;
    private boolean decrementDesiredCapacity = true;
    private DeployStrategyType deployStrategy = DeployStrategyType.KEEP_CAPACITY;
    private Integer maxCapacity = -1;
    private Integer minCapacity = 1;

    /**
     * AWS OpsWorks Properties
     **/
    private String opsWorksLayerId = null;


    public String getAutoScalingGroupId() {
        return autoScalingGroupId;
    }

    public boolean useAutoScaling() {
        return useAutoScaling;
    }

    public boolean useOpsWorks() {
        return useOpsWorks;
    }

    public boolean isDeploySnapshots() {
        return deploySnapshots;
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

    public String getTarget() {
        return target;
    }

    public boolean useElbStatusCheck() {
        return this.elb;
    }

    public boolean doRestart() {
        return restart;
    }

    public boolean getAwsPrivateIp() {
        return this.awsPrivateIp;
    }

    public String getOpsWorksLayerId() {
        return this.opsWorksLayerId;
    }

    public void setTestScope(boolean testScope) {
        this.testScope = testScope;
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

    public DeployConfiguration withAutoScalingGroup(String autoScalingGroup) {
        this.autoScalingGroupId = autoScalingGroup;
        return this;
    }

    public DeployConfiguration withStrategy(String strategy) {
        this.deployStrategy = DeployStrategyType.valueOf(strategy);
        return this;
    }

    public DeployConfiguration withMaxGroupSize(Integer maxGroupSize) {
        this.maxCapacity = maxGroupSize;
        return this;
    }

    public DeployConfiguration withMinGroupSize(Integer minGroupSize) {
        this.minCapacity = minGroupSize;
        return this;
    }

    public DeployConfiguration withElb(boolean useElb) {
        this.elb = useElb;
        return this;
    }

    public DeployConfiguration withPrivateIp(boolean usePrivateIp) {
        this.awsPrivateIp = usePrivateIp;
        return this;
    }

    public DeployConfiguration withTestScope(boolean isTestScope) {
        this.testScope = isTestScope;
        return this;
    }

    public DeployConfiguration withConfig(boolean deployConfig) {
        this.deployConfig = deployConfig;
        return this;
    }

    public DeployConfiguration withRestart(boolean doRestart) {
        this.restart = doRestart;
        return this;
    }

    public DeployConfiguration withDecrementCapacity(boolean decrementCapacity) {
        this.decrementDesiredCapacity = decrementCapacity;
        return this;
    }

    public DeployConfiguration withIgnoreInStandby(boolean ignoreInStandby) {
        this.ignoreInStandby = ignoreInStandby;
        return this;
    }

    public void setDeploySnapshots(boolean deploySnapshots) {
        this.deploySnapshots = deploySnapshots;
    }
}
