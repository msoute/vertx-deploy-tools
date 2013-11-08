package nl.soutenet.vertx.mod.cluster.util;

import nl.soutenet.vertx.mod.cluster.request.DeployRequest;

import java.io.File;
import java.io.FilenameFilter;

public class ModuleFileNameFilter implements FilenameFilter {

    private final DeployRequest deployRequest;

    public ModuleFileNameFilter(final DeployRequest deployRequest) {
        this.deployRequest = deployRequest;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.startsWith(deployRequest.getGroupId()+"~"+deployRequest.getArtifactId());
    }

}
