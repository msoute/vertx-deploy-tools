package nl.jpoint.vertx.mod.test;

import io.vertx.core.Vertx;

import static io.vertx.core.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static java.lang.Boolean.TRUE;

public class TestRunner {
    public static void main(String[] args) {
        System.setProperty(DISABLE_CP_RESOLVING_PROP_NAME, TRUE.toString());
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("nl.jpoint.vertx.mod.test.DeployTestApplication");
    }
}
