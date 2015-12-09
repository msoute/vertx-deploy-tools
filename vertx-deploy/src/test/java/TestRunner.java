import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static java.lang.Boolean.TRUE;

public class TestRunner {
    public static void main(String[] args) {
        System.setProperty(DISABLE_CP_RESOLVING_PROP_NAME, TRUE.toString());
        Vertx vertx = Vertx.vertx();
        JsonObject config = new JsonObject();
        config.put("vertx.home", "/home/marcel/Java/Tools/vert.x-3.1.0");
        //config.put("maven.repo.uri", "https://repo1.maven.org/maven2");
        config.put("artifact.storage", "/tmp");
        DeploymentOptions deployConf = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle("nl.jpoint.vertx.mod.deploy.AwsDeployApplication", deployConf);
    }
}