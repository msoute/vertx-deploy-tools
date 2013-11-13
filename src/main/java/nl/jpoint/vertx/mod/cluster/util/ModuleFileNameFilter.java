package nl.jpoint.vertx.mod.cluster.util;

import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;

import java.io.File;
import java.io.FilenameFilter;

public class ModuleFileNameFilter implements FilenameFilter {

    private final ModuleRequest deployRequest;

    public ModuleFileNameFilter(final ModuleRequest moduleRequest) {
        this.deployRequest = moduleRequest;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.startsWith(deployRequest.getGroupId() + "~" + deployRequest.getArtifactId());
    }

}
