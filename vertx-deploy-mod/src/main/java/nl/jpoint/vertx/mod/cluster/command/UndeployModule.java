package nl.jpoint.vertx.mod.cluster.command;

import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import nl.jpoint.vertx.mod.cluster.util.ModuleFileNameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.io.File;

public class UndeployModule implements Command<ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(UndeployModule.class);
    private final Vertx vertx;
    private final File modRoot;

    public UndeployModule(Vertx vertx, File modRoot) {
        this.vertx = vertx;
        this.modRoot = modRoot;
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        final JsonObject result = new JsonObject();

        for (String file : modRoot.list(new ModuleFileNameFilter(request))) {
            LOG.info("[{} - {}]: Undeploying module {}", LogConstants.DEPLOY_REQUEST, request.getId(), file);

            vertx.fileSystem().deleteSync(modRoot + "/" + file, true);
            LOG.info("[{} - {}]: Undeployed  module : {}", LogConstants.DEPLOY_REQUEST, request.getId(), file);
        }
        return result;
    }
}
