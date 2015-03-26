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
///home/marcel/Java/Projects/vertx-deploy-tools/vertx-deploy-mod/src/test/resources

        JsonObject config = new JsonObject();
        config.putString("aws.auth.access.key", "1");
        config.putString("aws.auth.secret.access.key", "1");
        config.putString("vertx.home", "src/test/resources/vertx");
        config.putString("mod.root", "/tmp/vertx/mods/");
        config.putString("artifact.repo", "/tmp/");
        config.putString("http.authUser", "edubase-build");
        config.putString("http.authPass", "aZV1zvdGZ1wXzeDajlRM");
        config.putString("http.authUri", "nexus.build.edubase.malmberg.nl");

        container.deployVerticle(ClusterManagerModule.class.getName(), config, handler);
    }

    @Test
    public void testInitialize() throws Exception {

        //testComplete();
    }


}

