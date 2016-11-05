package nl.jpoint.vertx.mod.deploy.util;


import nl.jpoint.vertx.mod.deploy.DeployConfig;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AetherUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AetherUtil.class);

    private AetherUtil() {
        // Hide
    }

    public static RepositorySystem newRepositorySystem() {
        /*
         * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
         * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
         * factories.
         */
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                LOG.error(exception.getMessage(), exception);
            }
        });
        return locator.getService(RepositorySystem.class);
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    public static List<RemoteRepository> newRepositories(DeployConfig deployConfig) {
        RemoteRepository nexus = newNexusRepo(deployConfig);
        if (nexus != null) {
            return Arrays.asList(nexus, newCentralRepository());
        } else {
            return new ArrayList<>(Collections.singletonList(newCentralRepository()));
        }
    }

    private static RemoteRepository newNexusRepo(DeployConfig deployConfig) {
        if (deployConfig.getNexusUrl() == null) {
            return null;
        }

        RemoteRepository.Builder builder = new RemoteRepository.Builder("nexus", "default", deployConfig.getNexusUrl().toString());
        if (deployConfig.isHttpAuthentication()) {
            builder.setAuthentication(new AuthenticationBuilder()
                    .addUsername(deployConfig.getHttpAuthUser())
                    .addPassword(deployConfig.getHttpAuthPassword())
                    .build());
        }

        return builder.build();
    }

    public static Model readPom(Artifact artifact) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            return reader.read(new FileReader(artifact.getFile()));
        } catch (IOException | XmlPullParserException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private static RemoteRepository newCentralRepository() {
        return new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
    }
}
