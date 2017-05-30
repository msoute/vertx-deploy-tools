package nl.jpoint.maven.vertx.model;

import org.eclipse.aether.artifact.Artifact;

public class DeployDependency {
    private String coordinates;
    private Artifact artifact;
    private Boolean test = false;

    public void withArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public Artifact getArtifact() {
        return this.artifact;
    }

    public Boolean isTest() {
        return this.test;
    }

    public String getCoordinates() {
        return this.coordinates;
    }

    // ** This wont work **/
    public Boolean isSnapshot() {
        return artifact.getBaseVersion().endsWith("-SNAPSHOT");
    }
}
