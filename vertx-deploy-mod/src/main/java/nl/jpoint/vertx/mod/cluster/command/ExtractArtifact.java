package nl.jpoint.vertx.mod.cluster.command;


import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
import nl.jpoint.vertx.mod.cluster.util.ArtifactContextUtil;
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

public class ExtractArtifact implements Command<ModuleRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractArtifact.class);
    private static final String ARTIFACT_CONTEXT = "artifact_context.xml";
    private final Vertx vertx;
    private final JsonObject config;

    public ExtractArtifact(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public JsonObject execute(ModuleRequest request) {

        try (FileSystem zipFs = this.getFilSystem(config.getString("artifact.repo") + "/" + request.getModuleId())) {

            Path path = zipFs.getPath(ARTIFACT_CONTEXT);

            ArtifactContextUtil artifactContextUtil = new ArtifactContextUtil(Files.readAllBytes(path));
            final Path basePath = Paths.get(artifactContextUtil.getBaseLocation());

            LOG.info("[{} - {}]: Extracting artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), basePath);
            if (!basePath.getParent().toFile().exists() || !basePath.getParent().toFile().canWrite()) {
                LOG.warn("[{} - {}]: Unable to extract artifact {} -> {} not exist or not writable.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), basePath.getParent().toString());
                LOG.warn("[{} - {}]: Unable to extract artifact {} to basePath -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), basePath.getParent().toFile().toString());
            }

            if (basePath.toFile().exists()) {
                LOG.info("[{} - {}]: Removing base path -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), basePath.toAbsolutePath());
                vertx.fileSystem().deleteSync(basePath.toString(), true);
            }

            final Path zipRoot = zipFs.getPath("/");

            Files.walkFileTree(zipRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (ARTIFACT_CONTEXT.equals(file.getFileName().toString())) {
                        return FileVisitResult.CONTINUE;
                    }
                    final Path unpackFile = Paths.get(basePath.toString(), file.toString());
                    Files.copy(file, unpackFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    final Path subDir = Paths.get(basePath.toString(), dir.toString());
                    if (Files.notExists(subDir)) {
                        Files.createDirectory(subDir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            LOG.info("[{} - {}]: Extracted artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), basePath);
        } catch (IOException | InvalidPathException e) {
            LOG.error("[{} - {}]: Error while extracting artifact {} -> {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), e.getMessage());
            return new JsonObject().putBoolean("success", false);
        }
        return new JsonObject().putBoolean("success", true);
    }

    private FileSystem getFilSystem(String location) throws IOException {
        Path path = Paths.get(location + ".zip");
        URI uri = URI.create("jar:file:" + path.toUri().getPath());
        return FileSystems.newFileSystem(uri, new HashMap<String, String>());
    }
}
