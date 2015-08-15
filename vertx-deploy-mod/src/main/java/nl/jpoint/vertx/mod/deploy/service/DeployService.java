package nl.jpoint.vertx.mod.deploy.service;

public interface DeployService<T> {
    boolean deploy(T deployRequest);

}
