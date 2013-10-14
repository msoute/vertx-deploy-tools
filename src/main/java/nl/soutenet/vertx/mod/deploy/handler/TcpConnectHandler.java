package nl.soutenet.vertx.mod.deploy.handler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

public class TcpConnectHandler implements Handler<NetSocket> {
    private NetSocket socket;

    private int count = 0;

    @Override
    public void handle(NetSocket socket) {
        this.socket = socket;
        System.out.println("Connection accepted " + socket.writeHandlerID());
        socket.dataHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                System.out.println("I received " + buffer.length() + " bytes of data");
                System.out.println(buffer.toString());
            }
        });
    }

    protected void done() {

    }
}
