package nl.jpoint.maven.vertx.model;

public class ArtifactDependency extends DeployDependency {
    private final ArtifactType artifactType = ArtifactType.ARTIFACT;

    private String path;
    private String testCommand;
    private String restartCommand;

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public String getPath() {
        return path;
    }

    public String getTestCommand() {
        return testCommand;
    }

    public String getRestartCommand() {
        return restartCommand;
    }
}
