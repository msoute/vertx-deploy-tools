package nl.jpoint.maven.vertx.utils;

public class Ec2Instance {
    private final String instanceId;
    private final String publicIp;
    private final String privateIp;

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
}
