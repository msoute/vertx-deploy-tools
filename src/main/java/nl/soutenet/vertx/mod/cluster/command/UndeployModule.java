package nl.soutenet.vertx.mod.cluster.command;

import nl.soutenet.vertx.mod.cluster.request.DeployRequest;
import nl.soutenet.vertx.mod.cluster.util.LogConstants;
import nl.soutenet.vertx.mod.cluster.util.ModuleFileNameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;

public class UndeployModule implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(UndeployModule.class);
    private static final String PID_DIR = "/var/run/edubase/";
    private final Vertx vertx;
    private final File modRoot;

    public UndeployModule(Vertx vertx, File modRoot, EventBus eventBus) {
        this.vertx = vertx;
        this.modRoot = modRoot;
    }

    @Override
    public JsonObject execute(DeployRequest request) {
        Process killProcess = null;

        for (String file : modRoot.list(new ModuleFileNameFilter(request))) {


            File pidFile = new File(PID_DIR + file);

            if (pidFile.exists()) {
                String pid = vertx.fileSystem().readFileSync(PID_DIR + file).toString();
                vertx.fileSystem().deleteSync(PID_DIR + file);
                try {
                    killProcess = Runtime.getRuntime().exec(new String[]{"kill", pid});
                    killProcess.waitFor();
                } catch (IOException | InterruptedException e) {
                    LOG.error("[{} - {}]: Unable to stop old module : {} with PID", LogConstants.DEPLOY_REQUEST, request.getId(), file, pid);
                }
            }

            vertx.fileSystem().deleteSync(modRoot.getPath() + "/" + file, true);
            LOG.info("[{} - {}]: Undeployed old module : {}", LogConstants.DEPLOY_REQUEST, request.getId(), file);
        }
        return null;
    }
}
