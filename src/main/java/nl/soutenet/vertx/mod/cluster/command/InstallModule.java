package nl.soutenet.vertx.mod.cluster.command;

import nl.soutenet.vertx.mod.cluster.Constants;
import nl.soutenet.vertx.mod.cluster.request.DeployRequest;
import nl.soutenet.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager;

import java.util.concurrent.atomic.AtomicInteger;

public class InstallModule implements Command {


    private static final Logger LOG = LoggerFactory.getLogger(InstallModule.class);

    private final PlatformManager platformManager;

    public InstallModule(PlatformManager platformManager, final EventBus eventbus) {
        this.platformManager = platformManager;
    }

    @Override
    public JsonObject execute(final DeployRequest request) {

        final AtomicInteger waitFor = new AtomicInteger(1);
        final JsonObject result = new JsonObject();
        LOG.info("[{} - {}]: Installing module {}", LogConstants.DEPLOY_REQUEST,request.getId().toString(), request.getModuleId());

        platformManager.installModule(request.getModuleId(), new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                if (event.failed()) {
                    LOG.error("[{} - {}]: Error installing module {} with cause {}", LogConstants.DEPLOY_REQUEST,request.getId().toString(), request.getModuleId(), event.cause().getMessage());
                    result.putBoolean(Constants.STATUS_SUCCESS, false);
                }  else {
                    result.putBoolean(Constants.STATUS_SUCCESS, true);
                    LOG.info("[{} - {}]: Installed module {}", LogConstants.DEPLOY_REQUEST, request.getId().toString(), request.getModuleId());
                }
                waitFor.decrementAndGet();
            }
        });

        while (waitFor.intValue() != 0) {
            // Wait for install
        }
        return result;

    }
}
