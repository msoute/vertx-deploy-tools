package nl.jpoint.vertx.mod.deploy.util;

import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(MockitoJUnitRunner.class)
public class GzipExtractorTest {

    private Path base = Paths.get("src/test/resources/test.tar.gz");

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private ModuleRequest request;

    @Test
    public void deflateGz() throws Exception {
        new GzipExtractor(request).deflateGz(base.resolve("test.tar.gz"));
    }

    @Test
    public void extractTar() throws Exception {
        new GzipExtractor<>(request).extractTar(base.resolve("test.tar"), base);
    }


}