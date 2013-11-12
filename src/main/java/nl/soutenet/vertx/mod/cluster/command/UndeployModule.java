package nl.soutenet.vertx.mod.cluster.command;

import nl.soutenet.vertx.mod.cluster.request.ModuleRequest;
import nl.soutenet.vertx.mod.cluster.util.LogConstants;
import nl.soutenet.vertx.mod.cluster.util.ModuleFileNameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;

public class UndeployModule implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(UndeployModule.class);
    private static final String PID_DIR = "/var/run/edubase/";
    private final Vertx vertx;
    private final File modRoot;

    public UndeployModule(Vertx vertx, File modRoot) {
        this.vertx = vertx;
        this.modRoot = modRoot;
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        Process killProcess = null;

        for (String file : modRoot.list(new ModuleFileNameFilter(request))) {

            try {
                killProcess = Runtime.getRuntime().exec(new String[]{"/etc/init.d/vertx", "stop", request.getModuleId()});
                killProcess.waitFor();
            } catch (IOException | InterruptedException e) {
                LOG.error("[{} - {}]: Failed to initialize module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
                return null;
            }

            vertx.fileSystem().deleteSync(modRoot.getPath() + "/" + file, true);
            LOG.info("[{} - {}]: Undeployed old module : {}", LogConstants.DEPLOY_REQUEST, request.getId(), file);
        }
        return null;
    }
}
