package nl.jpoint.vertx.mod.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class DeployTestApplication extends AbstractVerticle {


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        System.out.println("Verticle Started");
        startFuture.fail("ERRORED");
    }

    @Override
    public void stop() {
        System.out.println("Verticle Stopped");
    }
}
