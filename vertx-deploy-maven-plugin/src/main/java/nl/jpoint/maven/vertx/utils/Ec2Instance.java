package nl.jpoint.maven.vertx.utils;

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Ec2Instance {
    private final String instanceId;
    private final String publicIp;
    private final String privateIp;
    private AwsState state = AwsState.UNKNOWN;

    private Ec2Instance(final String instanceId, final String publicIp, final String privateIp) {
        this.instanceId = instanceId;
        this.publicIp = publicIp;
        this.privateIp = privateIp;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public AwsState getState() {
        return state;
    }

    public void updateState(AwsState awsState) {
        if (state.ordinal() < awsState.ordinal()) {
            state = awsState;
        }
    }

    public static class Builder {
        private String instanceId;
        private String publicIp;
        private String privateIp;

        public Builder withInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder withPublicIp(String ip) {
            this.publicIp = ip;
            return this;
        }

        public Builder withPrivateIp(String privateIp) {
            this.privateIp = privateIp;
            return this;
        }

        public Ec2Instance build() {
            return new Ec2Instance(instanceId, publicIp, privateIp);
        }
    }

    public boolean isReachable(boolean usePrivate, int port, Log log) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(usePrivate ? privateIp : publicIp, port), 5000);
            return socket.isConnected();
        } catch (IOException e) {
            log.error("Error while checking if instance "+ instanceId + " is reachable", e);
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Error while closing connection to "+ instanceId, e);
                }
            }
        }
    }
}