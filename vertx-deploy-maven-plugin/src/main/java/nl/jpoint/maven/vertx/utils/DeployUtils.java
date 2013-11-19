package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployRequest;
import nl.jpoint.maven.vertx.request.DeploySiteRequest;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

public class DeployUtils {

    private final Log log;
    private final MavenProject project;

    public DeployUtils(Log log, MavenProject project) {

        this.log = log;
        this.project = project;
    }

    public DeployConfiguration setActiveDeployConfig(List<DeployConfiguration> configurations, String target) throws MojoFailureException {
        DeployConfiguration activeConfiguration = null;
        if (configurations.size() == 1) {
            log.info("Found exactly one deploy config to activate.");
            activeConfiguration = configurations.get(0);
        } else {
            for (DeployConfiguration config : configurations) {
                if (target.equals(config.getTarget())) {
                    activeConfiguration = config;
                    break;
                }
            }
        }

        if (activeConfiguration == null) {
            log.error("No active deployConfig !");
            throw new MojoFailureException("No active deployConfig !, config should contain at least one config with scope default");
        }

        log.info("Deploy config with target " + activeConfiguration.getTarget() + " activated");
        return activeConfiguration;
    }

    public List<Request> createDeploySiteList(DeployConfiguration activeConfiguration, String siteClassifier) throws MojoFailureException {
        List<Request> deployModuleRequests = new ArrayList<>();
        for (Dependency dependency :createDeployList(activeConfiguration, siteClassifier)) {
            deployModuleRequests.add(new DeploySiteRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), activeConfiguration.getSiteBasePath()));
        }
        return deployModuleRequests;
    }

    public List<Request> createDeployModuleList(DeployConfiguration activeConfiguration, String classifier) throws MojoFailureException {
        List<Request> deployModuleRequests = new ArrayList<>();
        for (Dependency dependency :createDeployList(activeConfiguration, classifier)) {
            deployModuleRequests.add(new DeployRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), 4));
        }
        return deployModuleRequests;
    }

    private List<Dependency> createDeployList(DeployConfiguration activeConfiguration, String classifier) throws MojoFailureException {

        List<Dependency> deployModuleDependencies = new ArrayList<>();

        List<Dependency> dependencies = project.getDependencies();

        for (Dependency dependency : dependencies) {

            if (dependency.getVersion().endsWith("-SNAPSHOT") && !activeConfiguration.isDeploySnapshots()) {
                throw new MojoFailureException("Target does not allow for snapshots to be deployed");
            }

            if (classifier.equals(dependency.getClassifier()) && !excluded(activeConfiguration, dependency) || inScope(activeConfiguration.isTestScope(), dependency, classifier)) {
                deployModuleDependencies.add(dependency);
            }
        }
        return deployModuleDependencies;
    }

    private boolean excluded(DeployConfiguration activeConfiguration, Dependency dependency) {
        if (activeConfiguration.getExclusions() == null) {
            return false;
        }
        for (Exclusion exclusion : activeConfiguration.getExclusions()) {
            if (exclusion.getArtifactId().equals(dependency.getArtifactId()) &&
                    exclusion.getGroupId().equals(dependency.getGroupId())) {
                log.info("Excluding dependency " + dependency.getArtifactId());
                return true;
            }
        }
        return false;
    }

    private boolean inScope(boolean useTestScope, Dependency dependency, String classifier) {
        if (useTestScope && classifier.equals(dependency.getClassifier()) && Artifact.SCOPE_TEST.equals(dependency.getScope())) {
            log.info("Including artifact " + dependency.getArtifactId() + " from scope " + dependency.getScope());
            return true;
        }
        log.info("Excluding artifact " + dependency.getArtifactId() + " from scope " + dependency.getScope());
        return false;
    }
}
