package nl.jpoint.maven.vertx.service;

import org.apache.maven.plugin.logging.Log;

abstract class DeployService {

    private final Log log;

    DeployService(Log log) {
        this.log = log;
    }

    Log getLog() {
        return log;
    }
}


