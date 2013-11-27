package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.model.Exclusion;

import java.util.ArrayList;
import java.util.List;

public class DeployConfiguration {

    private List<String> dirsToClean = new ArrayList<>();

    private List<String> hosts;
    private String target;

    private String context;

    private List<Exclusion> exclusions;

    private boolean deploySnapshots = true;
    private boolean testScope = false;

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
}
