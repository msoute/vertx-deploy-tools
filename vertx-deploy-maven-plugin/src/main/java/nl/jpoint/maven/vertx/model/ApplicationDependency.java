package nl.jpoint.maven.vertx.model;

public class ApplicationDependency extends DeployDependency {
    private final ArtifactType artifactType = ArtifactType.SERVICE;

    public ArtifactType getArtifactType() {
        return artifactType;
    }

}
