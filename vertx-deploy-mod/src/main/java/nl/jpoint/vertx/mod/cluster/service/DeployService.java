package nl.jpoint.vertx.mod.cluster.service;

public interface DeployService<T> {
    boolean deploy(T deployRequest);

}
