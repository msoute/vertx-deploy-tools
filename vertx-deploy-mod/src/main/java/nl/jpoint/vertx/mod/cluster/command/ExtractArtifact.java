package nl.jpoint.vertx.mod.cluster.command;


import nl.jpoint.vertx.mod.cluster.request.DeployArtifactRequest;
import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

public class ExtractArtifact implements Command {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractArtifact.class);
    private final Vertx vertx;
    private JsonObject config;

    public ExtractArtifact(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public JsonObject execute(ModuleRequest request) {
        boolean hasOld = false;

        final Path contextPath = Paths.get(((DeployArtifactRequest) request).getContext());

        LOG.info("[{} - {}]: Extracting artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), contextPath);
        if (!contextPath.getParent().toFile().exists() || !contextPath.getParent().toFile().canWrite()) {
            LOG.error("[{} - {}]: Unable to extract artifact {} -> {} not exist or not writable.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), contextPath.getParent().toString());
            return new JsonObject().putBoolean("success", false);
        }

        Path oldContextPath = Paths.get(contextPath.toString()+"_old");

        if (oldContextPath.toFile().exists()) {
            LOG.warn("[{} - {}]: Old context path all ready exists. Removing.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId());
            vertx.fileSystem().deleteSync(oldContextPath.toString(), true);
        }

        if (contextPath.toFile().exists()) {
            hasOld = true;
            vertx.fileSystem().moveSync(contextPath.toString(), contextPath.toString() + "_old");
        }


        try (FileSystem zipFs = this.getFilSystem(config.getString("artifact.repo") + "/" + request.getModuleId())) {
            final Path zipRoot = zipFs.getPath("/");
            Files.walkFileTree(zipRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final Path unpackFile = Paths.get(contextPath.toString(), file.toString());
                    Files.copy(file, unpackFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    final Path subDir = Paths.get(contextPath.toString(), dir.toString());
                    if (Files.notExists(subDir)) {
                        //vertx.fileSystem().mkdirSync(subDir.toString());
                        Files.createDirectory(subDir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.error("[{} - {}]: Error while extracting artifact {} -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), e.getMessage());
            if (hasOld) {
                vertx.fileSystem().moveSync(contextPath.toString() + "_old", contextPath.toString());
            }
            return new JsonObject().putBoolean("success", false);
        }

        if (hasOld) {
            vertx.fileSystem().deleteSync(contextPath.toString() + "_old", true);
        }
        LOG.info("[{} - {}]: Extracted artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), contextPath);
        return new JsonObject().putBoolean("success", true);
    }

    private FileSystem getFilSystem(String location) throws IOException {
        Path path = Paths.get(location + ".zip");
        URI uri = URI.create("jar:file:" + path.toUri().getPath());
        return FileSystems.newFileSystem(uri, new HashMap<String, String>());
    }
}
