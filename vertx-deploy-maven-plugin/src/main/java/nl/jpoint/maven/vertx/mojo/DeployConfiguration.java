package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.model.Exclusion;

import java.util.ArrayList;
import java.util.List;

public class DeployConfiguration {

    private List<String> dirsToClean = new ArrayList<>();

    private List<String> hosts = new ArrayList<>();
    private String target;

    private String context;
    private boolean awsPrivateIp = false;

    public String getOpsWorksStackId() {
        return opsWorksStackId;
    }

    private String opsWorksStackId = null;
    private String opsWorksLayerId = null;

    private List<Exclusion> exclusions;

    private boolean deploySnapshots = true;
    private boolean testScope = false;
    private boolean aws = false;

    private boolean restart = false;
    private boolean opsWorks = false;

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

    public boolean getAws() {
        return this.aws;
    }

    public boolean getOpsWorks() {
        return this.opsWorks;
    }

    public boolean doRestart() { return restart; }

    public boolean getAwsPrivateIp() {
        return this.awsPrivateIp;
    }

    public String getOpsWorksLayerId() {
        return this.opsWorksLayerId;
    }

    public void setTestScope(boolean testScope) {
        this.testScope = testScope;
    }
}
