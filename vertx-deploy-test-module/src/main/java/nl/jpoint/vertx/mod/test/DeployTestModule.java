package nl.jpoint.vertx.mod.test;

import org.vertx.java.platform.Verticle;

public class DeployTestModule extends Verticle {

    @Override
    public void start() {
        getContainer().logger().info("Verticle started");
    }

    @Override
    public void stop() {
        getContainer().logger().info("Verticle Stopped");
    }
}
