package nl.jpoint.vertx.mod.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployTestApplication extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(DeployTestApplication.class);


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOG.info("Verticle Started");
        startFuture.complete();
    }

    @Override
    public void stop() {
        LOG.info("Verticle Stopped");
    }
}
