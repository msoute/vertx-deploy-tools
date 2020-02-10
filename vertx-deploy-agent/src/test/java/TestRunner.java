import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.file.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static java.lang.Boolean.TRUE;

public class TestRunner {
    public static void main(String[] args) {
        System.setProperty(DISABLE_CP_RESOLVING_PROP_NAME, TRUE.toString());
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        Vertx vertx = Vertx.vertx();
        JsonObject config = new JsonObject();
        config.put("vertx.home", "/home/marcel/Java/Tools/vertx-3.5.1");
        config.put("artifact.storage", "/tmp/");
        config.put("auth.token", "vertx");
        config.put("vertx.maven.remoteRepos", "https://oss.sonatype.org/content/repositories/snapshots/");
        config.put("vertx.maven.remoteSnapshotPolicy", "always");
        DeploymentOptions deployConf = new DeploymentOptions().setConfig(config);
        vertx.deployVerticle("maven:nl.jpoint.vertx-deploy-tools:vertx-deploy-agent:3.5.0-SNAPSHOT", deployConf);
        //   vertx.deployVerticle("maven:nl.jpoint.vertx-deploy-tools:vertx-deploy:3.5.0", deployConf);
    }
}
