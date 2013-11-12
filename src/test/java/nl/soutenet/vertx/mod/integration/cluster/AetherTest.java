/*
package nl.soutenet.vertx.mod.integration.cluster;

import org.apac
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.junit.Test;

public class AetherTest {
    @Test
    public void downloadArtifact() throws ArtifactResolutionException {


        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        RepositorySystem system = locator.getService(RepositorySystem.class);
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository( "target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        Artifact artifact = new DefaultArtifact("org.sonatype.aether:aether-util:1.9");

        RemoteRepository repo = new RemoteRepository.Builder( "central", "default", "http://repo1.maven.org/maven2/" ).build();

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.addRepository(repo);

        ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);

        artifact = artifactResult.getArtifact();

        System.out.println(artifact + " resolved to  " + artifact.getFile());

    }
}
*/
