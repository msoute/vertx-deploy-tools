package nl.soutenet.vertx.mod.integration.cluster;

import nl.jpoint.vertx.mod.cluster.ClusterManagerModule;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import static org.vertx.testtools.VertxAssert.fail;
import static org.vertx.testtools.VertxAssert.testComplete;

public class VertxModuleTest extends TestVerticle {

    @Override
    public void start(final Future<Void> startResult) {
        VertxAssert.initialize(getVertx());
        Handler<AsyncResult<String>> handler = new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if (event.failed()) {
                    getContainer().logger().error(event.cause().getMessage(), event.cause());
                    fail();
                }
                VertxModuleTest.super.start();
                startResult.setResult(null);
            }
        };

        JsonObject config = new JsonObject();
        config.putString("aws.auth.access.key", "1");
        config.putString("aws.auth.secret.access.key", "1");
        config.putString("vertx.home", "src/test/resources/vertx");
        config.putString("mod.root", "/tmp/vertx/mods/");
        config.putString("artifact.repo", "/tmp/");

        container.deployVerticle(ClusterManagerModule.class.getName(), config, handler);
    }

    @Test
    public void testInitialize() throws Exception {
        testComplete();
    }


}

