import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static java.lang.Boolean.TRUE;

public class TestRunner {
    public static void main(String[] args) {
        System.setProperty(DISABLE_CP_RESOLVING_PROP_NAME, TRUE.toString());
        System.setProperty("vertx.logger-delegate-factory-class-name","io.vertx.core.logging.SLF4JLogDelegateFactory");
        Vertx vertx = Vertx.vertx();
        JsonObject config = new JsonObject();
        config.put("vertx.home", "/home/marcel/Java/Tools/vert.x-3.1.0");
        config.put("artifact.storage", "/tmp/");
        config.put("auth.token","token");
        config.put("maven.repo.uri", "https://oss.sonatype.org/content/repositories/snapshots/");
        DeploymentOptions deployConf = new DeploymentOptions().setConfig(config);
        vertx.deployVerticle("nl.jpoint.vertx.mod.deploy.AwsDeployApplication", deployConf);
    }
}