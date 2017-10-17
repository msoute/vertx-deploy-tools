package nl.jpoint.maven.vertx.utils;

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class InstanceUtils {
    private InstanceUtils() {
        // Hide
    }

    public static boolean isReachable(String ip, int port, String instanceId, Log log) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 5000);
            return socket.isConnected();
        } catch (IOException e) {
            log.warn("Error while checking if instance " + instanceId + " is reachable : " + e.getMessage());
            return false;
        }
    }
}
