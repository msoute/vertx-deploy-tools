package nl.jpoint.vertx.deploy.agent.service;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.aws.AwsAutoScalingUtil;
import nl.jpoint.vertx.deploy.agent.request.*;
import nl.jpoint.vertx.deploy.agent.util.AetherUtil;
import nl.jpoint.vertx.deploy.agent.util.DeployType;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    private final Vertx vertx;

    public AutoDiscoverDeployService(DeployConfig config, DefaultDeployService defaultDeployService, Vertx vertx) {
        this.deployConfig = config;
        this.awsAutoScalingUtil = new AwsAutoScalingUtil(config);
        this.defaultDeployService = defaultDeployService;
        this.vertx = vertx;
        this.system = AetherUtil.newRepositorySystem();
        this.session = AetherUtil.newRepositorySystemSession(system);
    }

    public void autoDiscoverFirstDeploy() {

        if (vertx.fileSystem().existsBlocking(deployConfig.getStatFile())) {
            LOG.info("Not initial run, skipping auto discover deploy");
            return;
        }

        Map<String, String> tags = awsAutoScalingUtil.getDeployTags();
        final boolean testScope;
        List<Artifact> dependencies = new ArrayList<>();

        if (deployConfig.isTypedDeploy()) {
            testScope = Boolean.parseBoolean(tags.getOrDefault(DeployType.APPLICATION.getScopeTag(), "false")) ||
                    Boolean.parseBoolean(tags.getOrDefault(DeployType.ARTIFACT.getScopeTag(), "false"));
            dependencies.addAll(getDependenciesForDeployType(tags, DeployType.APPLICATION, testScope));
            dependencies.addAll(getDependenciesForDeployType(tags, DeployType.ARTIFACT, testScope));
        } else {
            testScope = Boolean.parseBoolean(tags.getOrDefault(DeployType.DEFAULT.getScopeTag(), "false"));
            dependencies.addAll(getDependenciesForDeployType(tags, DeployType.DEFAULT, testScope));
        }

        if (!dependencies.isEmpty()) {
            DeployRequest request = this.createAutoDiscoverDeployRequest(dependencies, testScope);
            LOG.info("[{}] : Starting auto discover deploy ", request.getId());
            just(request)
                    .flatMap(x -> defaultDeployService.deployConfigs(request.getId(), request.getConfigs()))
                    .flatMap(x -> defaultDeployService.deployArtifacts(request.getId(), request.getArtifacts()))
                    .flatMap(x -> defaultDeployService.deployApplications(request.getId(), request.getModules()))
                    .doOnError(t -> LOG.error("[{}] : Error while performing auto discover deploy {}", request.getId(), t))
                    .doOnCompleted(() -> {
                        LOG.info("[{}] : Completed auto discover deploy.", request.getId());
                        vertx.fileSystem().createFileBlocking(deployConfig.getStatFile());
                    })
                    .subscribe();
        }

    }

    private List<Artifact> getDependenciesForDeployType(Map<String, String> tags, DeployType type, boolean testScope) {
        if (!tags.containsKey(type.getLatestRequestTag()) || tags.get(type.getLatestRequestTag()) == null || tags.get(type.getLatestRequestTag()).isEmpty()) {
            LOG.info("No tag {} in auto scaling group.", type.getLatestRequestTag());
            return new ArrayList<>();
        }
        Artifact deployArtifact = getDeployArtifact(tags.get(type.getLatestRequestTag()));
        List<Artifact> dependencies = new ArrayList<>();
        if (deployArtifact != null) {
            dependencies.addAll(getDeployDependencies(deployArtifact,
                    getExclusions(tags.getOrDefault(type.getExclusionTag(), "")),
                    testScope,
                    getProperties(tags.getOrDefault(type.getPropertiesTag(), "")), type));
            dependencies.forEach(a -> LOG.trace("{}:{}:{}:{}", a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getVersion()));
        }

        return dependencies;
    }

    private DeployRequest createAutoDiscoverDeployRequest(List<Artifact> dependencies, boolean testScope) {
        List<DeployConfigRequest> configs = dependencies.stream()
                .filter(a -> ModuleRequest.CONFIG_TYPE.equals(a.getExtension()))
                .map(a -> DeployConfigRequest.build(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier()))
                .collect(Collectors.toList());

        List<DeployArtifactRequest> artifacts = dependencies.stream()
                .filter(a -> ModuleRequest.ZIP_TYPE.equals(a.getExtension()) || ModuleRequest.GZIP_TYPE.equals(a.getExtension()))
                .map(a -> DeployArtifactRequest.build(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getExtension()))
                .collect(Collectors.toList());

        List<DeployApplicationRequest> applications = dependencies.stream()
                .filter(a -> "jar".equals(a.getExtension()))
                .map(a -> DeployApplicationRequest.build(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), testScope))
                .collect(Collectors.toList());

        return new DeployRequest(applications, artifacts, configs, false, false, "", false, testScope);
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
            LOG.error("Unable to resolve deploy artifact '{}', unable to auto-discover ", mavenCoords, e);
        }
        return null;
    }

    private List<Artifact> getDeployDependencies(Artifact artifact, List<Exclusion> exclusions, boolean testScope, Map<String, String> properties, DeployType type) {
        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setRepositories(AetherUtil.newRepositories(deployConfig));
        descriptorRequest.setArtifact(artifact);
        Model model = AetherUtil.readPom(artifact);
        if (model == null) {
            throw new IllegalStateException("Unable to read POM for " + artifact.getFile());
        }
        try {
            ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor(session, descriptorRequest);

            return descriptorResult.getDependencies().stream()
                    .filter(d -> type == DeployType.DEFAULT || (type == DeployType.APPLICATION && !d.getArtifact().getExtension().equals("zip")) || (type == DeployType.ARTIFACT && !d.getArtifact().getExtension().equals("jar")))
                    .filter(d -> "compile".equalsIgnoreCase(d.getScope()) || ("test".equalsIgnoreCase(d.getScope()) && testScope))
                    .filter(d -> !exclusions.contains(new Exclusion(d.getArtifact().getGroupId(), d.getArtifact().getArtifactId(), null, null)))
                    .map(Dependency::getArtifact)
                    .map(d -> this.checkWithModel(model, d, properties))
                    .collect(Collectors.toList());

        } catch (ArtifactDescriptorException e) {
            LOG.error("Unable to resolve dependencies for deploy artifact '{}', unable to auto-discover ", artifact, e);
        }
        return Collections.emptyList();
    }

    private Artifact checkWithModel(Model model, Artifact artifact, Map<String, String> properties) {
        Optional<org.apache.maven.model.Dependency> result = model.getDependencies().stream()
                .filter(d -> d.getGroupId().equals(artifact.getGroupId()))
                .filter(d -> d.getArtifactId().equals(artifact.getArtifactId()))
                .filter(d -> d.getClassifier() != null && properties.containsKey(d.getClassifier().substring(2, d.getClassifier().length() - 1)))
                .findFirst();
        return result.isPresent() ?
                new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), properties.get(result.get().getClassifier().substring(2, result.get().getClassifier().length() - 1)), artifact.getExtension(), artifact.getVersion(), artifact.getProperties(), artifact.getFile())
                : artifact;
    }

    private List<Exclusion> getExclusions(String exclusionString) {
        if (exclusionString == null || exclusionString.isEmpty()) {
            return Collections.emptyList();
        }
        return Stream.of(exclusionString.split(";"))
                .map(this::toExclusion)
                .collect(Collectors.toList());
    }

    private Map<String, String> getProperties(String propertiesString) {
        if (propertiesString == null || propertiesString.isEmpty()) {
            return Collections.emptyMap();
        }

        return Stream.of(propertiesString.split(";"))
                .filter(s -> s.contains(":"))
                .map(s -> s.split(":"))
                .filter(s -> s.length == 2)
                .collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));

    }

    private Exclusion toExclusion(String s) {
        String[] ex = s.split(":", 2);
        return new Exclusion(ex[0], ex[1], null, null);
    }

}