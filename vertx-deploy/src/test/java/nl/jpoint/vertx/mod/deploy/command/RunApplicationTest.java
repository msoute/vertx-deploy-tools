package nl.jpoint.vertx.mod.deploy.command;

import io.vertx.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.DeployApplicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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
        assertThat(result, hasSize(0));

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