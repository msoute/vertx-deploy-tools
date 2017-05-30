package nl.jpoint.maven.vertx.utils;

import nl.jpoint.maven.vertx.model.ApplicationDependency;
import nl.jpoint.maven.vertx.model.ArtifactDependency;
import nl.jpoint.maven.vertx.model.ConfigDependency;
import nl.jpoint.maven.vertx.model.DeployDependency;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import nl.jpoint.maven.vertx.request.DeployApplicationRequest;
import nl.jpoint.maven.vertx.request.DeployArtifactRequest;
import nl.jpoint.maven.vertx.request.DeployConfigRequest;
import nl.jpoint.maven.vertx.request.Request;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeployUtils {

    private final Log log;
    private final List<RemoteRepository> remoteRepos;
    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;

    public DeployUtils(Log log, List<RemoteRepository> remoteRepos, RepositorySystem repoSystem, RepositorySystemSession repoSession) {

        this.log = log;
        this.remoteRepos = remoteRepos;
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
    }

    public List<Request> createServiceDeployRequest(DeployConfiguration activeConfiguration, MavenProject project) {
        Request request = new DeployApplicationRequest(project.getArtifact().getGroupId(), project.getArtifact().getArtifactId(), project.getArtifact().getVersion(), project.getArtifact().getClassifier(), project.getArtifact().getType(), activeConfiguration.doRestart());
        return Collections.singletonList(request);
    }

    public List<Request> createDeployApplicationList(List<ApplicationDependency> applicationDependencies, DeployConfiguration activeConfiguration) throws MojoFailureException {
        return createDeployList(applicationDependencies, activeConfiguration).stream()
                .map(dependency -> new DeployApplicationRequest(dependency.getArtifact(), false)).collect(Collectors.toList());
    }

    public List<Request> createDeployArtifactList(List<ArtifactDependency> artifactDependencies, DeployConfiguration activeConfiguration) throws MojoFailureException {
        return createDeployList(artifactDependencies, activeConfiguration).stream()
                .map(dependency -> new DeployArtifactRequest(dependency.getArtifact())).collect(Collectors.toList());
    }

    public List<Request> createDeployConfigList(List<ConfigDependency> configDependencies, DeployConfiguration activeConfiguration) throws MojoFailureException {
        return createDeployList(configDependencies, activeConfiguration).stream()
                .map(dependency -> new DeployConfigRequest(dependency.getArtifact())).collect(Collectors.toList());
    }

    public List<String> parseProperties(String properties) {
        if (StringUtils.isBlank(properties)) {
            return new ArrayList<>();
        }
        return Pattern.compile(";").splitAsStream(properties).collect(Collectors.toList());
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

    private List<DeployDependency> createDeployList(List<? extends DeployDependency> deployDependencies, DeployConfiguration activeConfiguration) throws MojoFailureException {
        List<DeployDependency> deployModuleDependencies = new ArrayList<>();

        Iterator<? extends DeployDependency> it = deployDependencies.iterator();

        filterTestArtifacts(activeConfiguration, it);

        for (DeployDependency dependency : deployDependencies) {
            resolveDependency(dependency);
            if ((dependency.isSnapshot() || hasTransitiveSnapshots(dependency)) && !activeConfiguration.isDeploySnapshots()) {
                throw new MojoFailureException("Target does not allow for snapshots to be deployed");
            }

            if (!excluded(activeConfiguration, dependency)) {
                deployModuleDependencies.add(dependency);
            }

        }
        return deployModuleDependencies;
    }

    private void resolveDependency(DeployDependency deployDependency) {
        ArtifactRequest request = new ArtifactRequest(new DefaultArtifact(deployDependency.getCoordinates()), remoteRepos, null);
        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
            deployDependency.withArtifact(result.getArtifact());
        } catch (ArtifactResolutionException e) {
            log.error(e);
        }
    }

    private boolean hasTransitiveSnapshots(DeployDependency dependency) throws MojoFailureException {
        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact(
                dependency.getArtifact());
        descriptorRequest.setRepositories(remoteRepos);

        try {
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor(repoSession, descriptorRequest);
            Optional<org.eclipse.aether.graph.Dependency> snapshotDependency = result.getDependencies().stream()
                    .filter(d -> d.getArtifact().isSnapshot())
                    .findFirst();
            return snapshotDependency.isPresent();
        } catch (ArtifactDescriptorException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private boolean excluded(DeployConfiguration activeConfiguration, DeployDependency dependency) {
        if (activeConfiguration.getExclusions() == null) {
            return false;
        }
        for (Exclusion exclusion : activeConfiguration.getExclusions()) {
            if (exclusion.getArtifactId().equals(dependency.getArtifact().getArtifactId()) &&
                    exclusion.getGroupId().equals(dependency.getArtifact().getGroupId())) {
                log.info("Excluding dependency " + dependency.getArtifact().getArtifactId());
                return true;
            }
        }
        return false;
    }

    private void filterTestArtifacts(DeployConfiguration activeConfiguration, Iterator<? extends DeployDependency> it) {
        if (!activeConfiguration.isTestScope()) {
            while (it.hasNext()) {
                DeployDependency dependency = it.next();
                if (dependency.isTest()) {
                    log.info("Excluding artifact " + dependency.getArtifact().getArtifactId() + " from scope tests");
                    it.remove();
                }
            }
        }
    }


}
