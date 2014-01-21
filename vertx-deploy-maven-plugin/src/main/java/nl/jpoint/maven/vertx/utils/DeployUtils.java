package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployArtifactRequest;
import nl.jpoint.maven.vertx.request.DeployModuleRequest;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeployUtils {

    private final Log log;
    private final MavenProject project;

    public DeployUtils(Log log, MavenProject project) {

        this.log = log;
        this.project = project;
    }

    public List<Request> createDeploySiteList(DeployConfiguration activeConfiguration, String siteClassifier) throws MojoFailureException {
        List<Request> deployModuleRequests = new ArrayList<>();
        for (Dependency dependency :createDeployList(activeConfiguration, siteClassifier)) {
            deployModuleRequests.add(new DeployArtifactRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getClassifier(), activeConfiguration.getContext()));
        }
        return deployModuleRequests;
    }

    public List<Request> createDeployModuleList(DeployConfiguration activeConfiguration, String classifier, boolean doRestart) throws MojoFailureException {
        List<Request> deployModuleRequests = new ArrayList<>();
        for (Dependency dependency :createDeployList(activeConfiguration, classifier)) {
            deployModuleRequests.add(new DeployModuleRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), 4, doRestart));
        }
        return deployModuleRequests;
    }

    private List<Dependency> createDeployList(DeployConfiguration activeConfiguration, String classifier) throws MojoFailureException {

        List<Dependency> deployModuleDependencies = new ArrayList<>();

        List<Dependency> dependencies = project.getDependencies();

        Iterator<Dependency> it = dependencies.iterator();

        if (!activeConfiguration.isTestScope()) {
            while(it.hasNext()) {
                Dependency dependency = it.next();
                if (Artifact.SCOPE_TEST.equals(dependency.getScope())) {
                    log.info("Excluding artifact " + dependency.getArtifactId() + " from scope " + dependency.getScope());
                    it.remove();
                }
            }
        }

        for (Dependency dependency : dependencies) {

            if (dependency.getVersion().endsWith("-SNAPSHOT") && !activeConfiguration.isDeploySnapshots()) {
                throw new MojoFailureException("Target does not allow for snapshots to be deployed");
            }

            if (classifier.equals(dependency.getClassifier()) && !excluded(activeConfiguration, dependency)) {
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
}
