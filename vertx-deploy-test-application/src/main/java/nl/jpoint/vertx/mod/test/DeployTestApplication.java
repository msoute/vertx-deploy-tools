package nl.jpoint.vertx.mod.test;

import io.vertx.core.AbstractVerticle;

public class DeployTestApplication extends AbstractVerticle {

    @Override
    public void start() {
       System.out.println("Verticle Started");
    }

    @Override
    public void stop() {
        System.out.println("Verticle Stopped");
    }
}
