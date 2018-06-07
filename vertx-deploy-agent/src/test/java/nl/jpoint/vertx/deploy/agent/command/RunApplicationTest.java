package nl.jpoint.vertx.deploy.agent.command;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.request.DeployApplicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RunApplicationTest {
    @Mock
    private DeployConfig config;
    @Mock
    private DeployApplicationRequest request;

    @Before
    public void init() {

        when(request.getGroupId()).thenReturn("group");
        when(request.getArtifactId()).thenReturn("artifact");
    }

    @Test
    public void testNoConfig() {
        when(config.getServiceConfigLocation()).thenReturn("runapplication/");
        List<DeployApplicationRequest> result = execute();
        assertThat(result, hasSize(1));

    }

    @Test
    public void testNewFormat() throws Exception {
        when(config.getServiceConfigLocation()).thenReturn("runapplication/newformat/");
        List<DeployApplicationRequest> result = execute();
        assertThat(result, hasSize(1));
        verify(request).withJavaOpts("newformat");
        verify(request).withInstances("2");
    }

    @Test
    public void testThatOldFormatIsUsed() throws Exception {
        when(config.getServiceConfigLocation()).thenReturn("runapplication/oldformat/");
        List<DeployApplicationRequest> result = execute();
        assertThat(result, hasSize(1));
        verify(request).withJavaOpts("test");
        verify(request).withInstances("2");
    }

    @Test
    public void testThatNewFormatIsPreferredOverOldFormat() throws Exception {
        when(config.getServiceConfigLocation()).thenReturn("runapplication/both/");
        List<DeployApplicationRequest> result = this.execute();
        assertThat(result, hasSize(1));
        verify(request).withJavaOpts("newformat");
        verify(request).withInstances("2");
    }

    @Test
    public void getOnNextEvents_oldFormat_requestToNotContainMainService() {
        when(config.getServiceConfigLocation()).thenReturn("runapplication/oldformat/");
        List<DeployApplicationRequest> result = execute();
        assertThat(result, hasSize(1));

        verify(request).withMainService("");
    }

    @Test
    public void getOnNextEvents_bothFormat_requestToNotContainMainService() {
        when(config.getServiceConfigLocation()).thenReturn("runapplication/both/");
        List<DeployApplicationRequest> result = execute();
        assertThat(result, hasSize(1));

        verify(request).withMainService("");
    }

    @Test
    public void getOnNextEvents_withMainService_requestToContainMainService() {
        when(config.getServiceConfigLocation()).thenReturn("runapplication/mainservice/");
        List<DeployApplicationRequest> result = execute();
        assertThat(result, hasSize(1));

        verify(request).withMainService("my-service");
    }

    @Test
    public void getMavenCommand_noMainService_normalMavenCommand() {
        String result = new RunApplication(Vertx.vertx(), config).getMavenCommand(new DeployApplicationRequest("group", "artifact", "version", "classifier", "type"));

        assertThat(result, is("maven:group:artifact:version"));
    }

    @Test
    public void getMavenCommand_withMainService_mavenCommandWithMainService() {
        String result = new RunApplication(Vertx.vertx(), config)
                .getMavenCommand(new DeployApplicationRequest("group", "artifact", "version", "classifier", "type")
                        .withMainService("main_service"));

        assertThat(result, is("maven:group:artifact:version::main_service"));
    }

    @Test
    public void getMavenCommand_withEmptyMainService_normalMavenCommand() {
        String result = new RunApplication(Vertx.vertx(), config)
                .getMavenCommand(new DeployApplicationRequest("group", "artifact", "version", "classifier", "type").withMainService(" "));

        assertThat(result, is("maven:group:artifact:version"));
    }

    private List<DeployApplicationRequest> execute() {
        RunApplication command = new RunApplication(Vertx.vertx(), config);
        Observable<DeployApplicationRequest> observable = command.readServiceDefaults(request);
        TestSubscriber<DeployApplicationRequest> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        return testSubscriber.getOnNextEvents();
    }

}
