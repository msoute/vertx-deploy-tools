package nl.jpoint.vertx.mod.cluster.service;

import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;

public interface DeployService<T> {
    public boolean deploy(T deployRequest);

}
