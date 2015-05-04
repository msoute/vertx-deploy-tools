package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployArtifactRequest;
import nl.jpoint.maven.vertx.request.DeployConfigRequest;
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
        for (Dependency dependency : createDeployListByClassifier(activeConfiguration, siteClassifier)) {
            deployModuleRequests.add(new DeployArtifactRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getClassifier(), dependency.getType(), activeConfiguration.getContext()));
        }
        return deployModuleRequests;
    }

    public List<Request> createDeployModuleList(DeployConfiguration activeConfiguration, String classifier) throws MojoFailureException {
        List<Request> deployModuleRequests = new ArrayList<>();
        for (Dependency dependency : createDeployListByClassifier(activeConfiguration, classifier)) {
            deployModuleRequests.add(new DeployModuleRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getType(), 4, activeConfiguration.doRestart()));
        }
        return deployModuleRequests;
    }

    public List<Request> createDeployConfigList(DeployConfiguration activeConfiguration, String type) throws MojoFailureException {
        List<Request> deployModuleRequests = new ArrayList<>();
        for (Dependency dependency : createDeployListByType(activeConfiguration, type)) {
            deployModuleRequests.add(new DeployConfigRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getClassifier(), dependency.getType()));
        }
        return deployModuleRequests;
    }

    private List<Dependency> createDeployListByClassifier(DeployConfiguration activeConfiguration, String classifier) throws MojoFailureException {

        List<Dependency> deployModuleDependencies = new ArrayList<>();

        List<Dependency> dependencies = project.getDependencies();

        Iterator<Dependency> it = dependencies.iterator();

        if (!activeConfiguration.isTestScope()) {
            while (it.hasNext()) {
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

    private List<Dependency> createDeployListByType(DeployConfiguration activeConfiguration, String type) throws MojoFailureException {

        List<Dependency> deployModuleDependencies = new ArrayList<>();

        List<Dependency> dependencies = project.getDependencies();

        Iterator<Dependency> it = dependencies.iterator();

        if (!activeConfiguration.isTestScope()) {
            while (it.hasNext()) {
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

            if (type.equals(dependency.getType()) && !excluded(activeConfiguration, dependency)) {
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
