package nl.jpoint.vertx.mod.deploy.service;

import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployConfigRequest;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.util.AetherUtil;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static rx.Observable.just;

public class AutoDiscoverDeployService {
    private static final Logger LOG = LoggerFactory.getLogger(AutoDiscoverDeployService.class);
    private final DeployConfig deployConfig;
    private final AwsAutoScalingUtil awsAutoScalingUtil;
    private final DefaultDeployService defaultDeployService;
    private final RepositorySystem system;
    private final RepositorySystemSession session;

    public AutoDiscoverDeployService(DeployConfig config, DefaultDeployService defaultDeployService) {
        this.deployConfig = config;
        this.awsAutoScalingUtil = new AwsAutoScalingUtil(config);
        this.defaultDeployService = defaultDeployService;
        this.system = AetherUtil.newRepositorySystem();
        this.session = AetherUtil.newRepositorySystemSession(system);
    }

    public void autoDiscoverFirstDeploy() {
        Map<String, String> tags = awsAutoScalingUtil.getDeployTags();

        if (!tags.containsKey(AwsAutoScalingUtil.LATEST_VERSION_TAG) || tags.get(AwsAutoScalingUtil.LATEST_VERSION_TAG) == null || tags.get(AwsAutoScalingUtil.LATEST_VERSION_TAG).isEmpty()) {
            LOG.info("No tag {} in auto scaling group.", AwsAutoScalingUtil.LATEST_VERSION_TAG);
            return;
        }

        Artifact deployArtifact = getDeployArtifact(tags.get(AwsAutoScalingUtil.LATEST_VERSION_TAG));

        if (deployArtifact != null) {
            List<Artifact> dependencies = getDeployDependencies(deployArtifact,
                    getExclusions(tags.getOrDefault(AwsAutoScalingUtil.EXCLUSION_TAG, "")),
                    Boolean.valueOf(tags.getOrDefault(AwsAutoScalingUtil.SCOPE_TAG, "false")));

            DeployRequest request = this.createAutoDiscoverDeployRequest(dependencies);
            LOG.info("[{}] : Starting auto discover deploy ", request.getId());
            just(request)
                    .flatMap(x -> defaultDeployService.deployConfigs(request.getId(), request.getConfigs()))
                    .flatMap(x -> defaultDeployService.deployArtifacts(request.getId(), request.getArtifacts()))
                    .flatMap(x -> defaultDeployService.deployApplications(request.getId(), request.getModules()))
                    .doOnError(t -> LOG.error("[{}] : Error while performing auto discover deploy {}", request.getId(), t))
                    .doOnCompleted(() -> LOG.info("[{}] : Completed auto discover deploy.", request.getId()))
                    .subscribe();
        }

    }

    private DeployRequest createAutoDiscoverDeployRequest(List<Artifact> dependencies) {
        List<DeployConfigRequest> configs = dependencies.stream()
                .filter(a -> "config".equals(a.getExtension()))
                .map(a -> DeployConfigRequest.build(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier()))
                .collect(Collectors.toList());

        List<DeployArtifactRequest> artifacts = dependencies.stream()
                .filter(a -> "zip".equals(a.getExtension()))
                .map(a -> DeployArtifactRequest.build(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier()))
                .collect(Collectors.toList());

        List<DeployApplicationRequest> applications = dependencies.stream()
                .filter(a -> "jar".equals(a.getExtension()))
                .map(a -> DeployApplicationRequest.build(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier()))
                .collect(Collectors.toList());

        return new DeployRequest(applications, artifacts, configs, false, false, "", false);
    }


    private Artifact getDeployArtifact(String mavenCoords) {
        Artifact artifact = new DefaultArtifact(mavenCoords);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(AetherUtil.newRepositories(deployConfig));

        try {
            ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
            return artifactResult.getArtifact();
        } catch (ArtifactResolutionException e) {
            LOG.error("Unable to resolve deploy artifact '{}', unable to auto-discover ", mavenCoords);
        }
        return null;
    }

    private List<Artifact> getDeployDependencies(Artifact artifact, List<Exclusion> exclusions, boolean testScope) {
        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setRepositories(AetherUtil.newRepositories(deployConfig));
        descriptorRequest.setArtifact(artifact);
        try {
            ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor(session, descriptorRequest);
            return descriptorResult.getDependencies().stream()
                    .filter(d -> "compile".equalsIgnoreCase(d.getScope()) || ("test".equalsIgnoreCase(d.getScope()) && testScope))
                    .filter(d -> !exclusions.contains(new Exclusion(d.getArtifact().getGroupId(), d.getArtifact().getArtifactId(), null, null)))
                    .map(Dependency::getArtifact)
                    .collect(Collectors.toList());
        } catch (ArtifactDescriptorException e) {
            LOG.error("Unable to resolve dependencies for deploy artifact '{}', unable to auto-discover ", artifact);
        }
        return Collections.emptyList();
    }

    private List<Exclusion> getExclusions(String exclusionString) {
        if (exclusionString == null || exclusionString.isEmpty()) {
            return Collections.emptyList();
        }
        return Stream.of(exclusionString.split(";"))
                .map(this::toExclusion)
                .collect(Collectors.toList());
    }

    private Exclusion toExclusion(String s) {
        String[] ex = s.split(":", 2);
        return new Exclusion(ex[0], ex[1], null, null);
    }
}