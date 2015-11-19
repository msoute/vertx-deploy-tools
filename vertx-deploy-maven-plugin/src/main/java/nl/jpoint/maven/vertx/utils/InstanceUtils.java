package nl.jpoint.maven.vertx.utils;

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InstanceUtils {
    public static boolean isReachable(String ip, int port, Log log) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 5000);
            return socket.isConnected();
        } catch (IOException e) {
            log.error("Error while checking if deploy module on ip  "+ ip + " is reachable", e);
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Error while closing connection to "+ ip, e);
                }
            }
        }
    }

}
