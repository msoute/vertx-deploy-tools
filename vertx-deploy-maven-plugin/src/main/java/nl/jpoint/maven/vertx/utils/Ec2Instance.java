package nl.jpoint.maven.vertx.utils;

import org.apache.maven.plugin.logging.Log;

public class Ec2Instance {
    private final String instanceId;
    private final String publicIp;
    private final String privateIp;
    private AwsState asState = AwsState.UNKNOWN;
    private AwsState elbState = AwsState.UNKNOWN;

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

    public AwsState getElbState() {
        return elbState;
    }

    public AwsState getAsState() {
        return this.asState;
    }

    public void updateState(AwsState awsState) {
        if (elbState.ordinal() < awsState.ordinal()) {
            elbState = awsState;
        }
    }

    public void updateAsState(AwsState awsState) {
        asState = awsState;
    }

    public boolean isReachable(boolean usePrivate, int port, Log log) {
        if (usePrivate ? privateIp == null : publicIp == null) {
            log.error("Instance has no IP, probably still booting");
            return false;
        }
        return InstanceUtils.isReachable(usePrivate ? privateIp : publicIp, port, instanceId, log);
    }

    public static class Builder {
        private String instanceId;
        private String publicIp;
        private String privateIp;
        private AwsState asState;

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
}