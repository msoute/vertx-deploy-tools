package nl.jpoint.maven.vertx.model;

public class ConfigDependency extends DeployDependency {
    private final ArtifactType artifactType = ArtifactType.CONFIG;

    private String path;

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public String getPath() {
        return path;
    }

}
