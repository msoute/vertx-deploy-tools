package nl.jpoint.maven.vertx.service;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;

public abstract class DeployService {

    private final Log log;
    private final Server server;

    public DeployService(Server server, Log log) {

        this.server = server;
        this.log = log;
    }

    public Log getLog() {
        return log;
    }

    public Server getServer() {
        return server;
    }
}
