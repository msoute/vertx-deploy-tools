package nl.soutenet.vertx.mod.deploy;

import nl.soutenet.vertx.mod.deploy.handler.TcpConnectHandler;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.platform.Verticle;

public class VertxDeployMod extends Verticle {

    @Override
    public void start() {
        NetServer server = getVertx().createNetServer();
        server.connectHandler(new TcpConnectHandler());
        server.listen(5678);
    }
}
