package nl.soutenet.vertx.mod.cluster.command;

import nl.soutenet.vertx.mod.cluster.Constants;
import nl.soutenet.vertx.mod.cluster.request.DeployRequest;
import nl.soutenet.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;

/**
 * TODO : Vertx homedir should be configurable.
 *
 */
public class RunModule implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(RunModule.class);


    @Override
    public JsonObject execute(final DeployRequest request) {
        LOG.info("[{} - {}] : Running module {}.", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());
        boolean success = false;

        try {
            final Process runProcess =  Runtime.getRuntime().exec(new String[]{"/etc/init.d/vertx", "start", request.getModuleId()});
            runProcess.waitFor();
            LOG.debug("{ }", runProcess.exitValue());
            success = true;
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]: Failed to initialize module {}", LogConstants.DEPLOY_REQUEST, request.getId(), request.getModuleId());
        }

        return new JsonObject()
                .putString(Constants.DEPLOY_ID, request.getId().toString())
                .putBoolean(Constants.STATUS_SUCCESS, success);
    }
}

