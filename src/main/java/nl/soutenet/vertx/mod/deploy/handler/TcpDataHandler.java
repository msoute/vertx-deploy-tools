package nl.soutenet.vertx.mod.deploy.handler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

public class TcpDataHandler implements Handler<Buffer> {
    private final TcpConnectHandler socketHandler;

    public TcpDataHandler(TcpConnectHandler tcpConnectHandler) {
        this.socketHandler = tcpConnectHandler;
    }

    @Override
    public void handle(Buffer buffer) {

        System.out.println("I received " + buffer.length() + " bytes of data");
        System.out.println(buffer.toString());
       // socketHandler.done();
    }
}
