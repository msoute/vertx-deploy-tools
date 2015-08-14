package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.model.Exclusion;

import java.util.ArrayList;
import java.util.List;

public class DeployConfiguration {
    private List<String> hosts = new ArrayList<>();
    private String target;

    private String context;
    private boolean awsPrivateIp = false;

    private boolean deployConfig = false;

    private boolean ignoreInStandby = false;
    private boolean ignoreDeployState = false;
    private boolean decrementDesiredCapacity = true;
    private boolean keepCurrentCapacity = true;
    private String tag;

    public String getOpsWorksStackId() {
        return opsWorksStackId;
    }

    private String opsWorksStackId = null;
    private String opsWorksLayerId = null;

    private List<Exclusion> exclusions;

    private boolean deploySnapshots = true;
    private boolean testScope = false;
    private boolean restart = false;

    private boolean opsWorks = false;
    private boolean autoScaling = false;
    private boolean elb = false;

    private String autoScalingGroupId;

    public String getAutoScalingGroupId() {
        return autoScalingGroupId;
    }

    public boolean isAutoScaling() {
        return autoScaling;
    }

    public boolean isOpsworks() {
        return opsWorks;
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

    public String getContext() {
        return context;
    }

    public boolean withElb() {
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

    public String getTag() {
        return tag;
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

    public void setIgnoreInStandby(boolean ignoreInStandby) {
        this.ignoreInStandby = ignoreInStandby;
    }

    public boolean isIgnoreDeployState() {
        return ignoreDeployState;
    }

    public boolean isKeepCurrentCapacity() {
        return keepCurrentCapacity;
    }

    public boolean isDecrementDesiredCapacity() {
        return decrementDesiredCapacity;
    }
}
