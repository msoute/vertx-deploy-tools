package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployArtifactRequest;
import nl.jpoint.maven.vertx.request.DeployConfigRequest;
import nl.jpoint.maven.vertx.request.DeployApplicationRequest;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeployUtils {
    public static final String APPLICATION_TYPE = "jar";
    public static final String ARTIFACT_TYPE = "zip";
    public static final String CONFIG_TYPE = "config";

    private final Log log;
    private final MavenProject project;



    public DeployUtils(Log log, MavenProject project) {

        this.log = log;
        this.project = project;
    }

    public List<Request> createDeployApplicationList(DeployConfiguration activeConfiguration) throws MojoFailureException {
        return createDeployListByType(activeConfiguration, APPLICATION_TYPE).stream().map(dependency -> new DeployApplicationRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getClassifier(), dependency.getType(), activeConfiguration.doRestart())).collect(Collectors.toList());
    }

    public List<Request> createDeployArtifactList(DeployConfiguration activeConfiguration) throws MojoFailureException {
        return createDeployListByType(activeConfiguration, ARTIFACT_TYPE).stream().map(dependency -> new DeployArtifactRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getClassifier(), dependency.getType())).collect(Collectors.toList());
    }

    public List<Request> createDeployConfigList(DeployConfiguration activeConfiguration) throws MojoFailureException {
        return createDeployListByType(activeConfiguration, CONFIG_TYPE).stream().map(dependency -> new DeployConfigRequest(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getClassifier(), dependency.getType())).collect(Collectors.toList());
    }

    public List<Exclusion> parseExclusions(String exclusions) {
        List<Exclusion> result = new ArrayList<>();
        if (StringUtils.isBlank(exclusions)) {
            return result;
        }

        Pattern.compile(";")
                .splitAsStream(exclusions)
                .forEach(s -> {
                            String[] mavenIds = Pattern.compile(":").split(s, 2);
                            if (mavenIds.length == 2) {
                                Exclusion exclusion = new Exclusion();
                                exclusion.setGroupId(mavenIds[0]);
                                exclusion.setArtifactId(mavenIds[1]);
                                result.add(exclusion);
                            }
                        }
                );
        return result;
    }

    private List<Dependency> createDeployListByType(DeployConfiguration activeConfiguration, String type) throws MojoFailureException {

        List<Dependency> deployModuleDependencies = new ArrayList<>();

        List<Dependency> dependencies = project.getDependencies();

        Iterator<Dependency> it = dependencies.iterator();

        FilterTestArtifacts(activeConfiguration, it);

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

    private void FilterTestArtifacts(DeployConfiguration activeConfiguration, Iterator<Dependency> it) {
        if (!activeConfiguration.isTestScope()) {
            while (it.hasNext()) {
                Dependency dependency = it.next();
                if (Artifact.SCOPE_TEST.equals(dependency.getScope())) {
                    log.info("Excluding artifact " + dependency.getArtifactId() + " from scope " + dependency.getScope());
                    it.remove();
                }
            }
        }
    }


}
