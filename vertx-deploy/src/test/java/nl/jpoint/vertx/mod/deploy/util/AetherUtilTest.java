package nl.jpoint.vertx.mod.deploy.util;

import nl.jpoint.vertx.mod.deploy.DeployConfig;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URISyntaxException;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AetherUtilTest {

    @Mock
    DeployConfig deployConfig;

    @Before
    public void init() throws URISyntaxException {
        when(deployConfig.getNexusUrl()).thenReturn(null);
        when(deployConfig.getHttpAuthUser()).thenReturn("");
        when(deployConfig.getHttpAuthPassword()).thenReturn("");
        when(deployConfig.isHttpAuthentication()).thenReturn(true);
    }

    @Test
    public void testResolveArtifact() {
        RepositorySystem system = AetherUtil.newRepositorySystem();

        RepositorySystemSession session = AetherUtil.newRepositorySystemSession(system);
        Artifact artifact = new DefaultArtifact("nl.malmberg.mathplus", "deploy", "pom", "1.0.0-SNAPSHOT");

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(AetherUtil.newRepositories(deployConfig));

        ArtifactResult artifactResult = null;

        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();

        descriptorRequest.setRepositories(AetherUtil.newRepositories(deployConfig));


        try {
            artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();
            descriptorRequest.setArtifact(artifact);
            ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor(session, descriptorRequest);

            for (Dependency dependency : descriptorResult.getDependencies()) {
                System.out.println(dependency);
            }

        } catch (ArtifactResolutionException e) {
            e.printStackTrace();
        } catch (ArtifactDescriptorException e) {
            e.printStackTrace();
        }

        System.out.println(artifact + " resolved to  " + artifact.getFile());
    }

}