package nl.jpoint.vertx.mod.deploy.command;


import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.ArtifactContextUtil;
import nl.jpoint.vertx.mod.deploy.util.FileDigestUtil;
import nl.jpoint.vertx.mod.deploy.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;

public class ExtractArtifact implements Command<ModuleRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractArtifact.class);

    private final Vertx vertx;
    private final JsonObject config;
    private final Path basePath;
    private final boolean deleteBase;
    private final String logConstant;
    private final FileDigestUtil fileDigestUtil;

    private boolean configChanged = false;

    public ExtractArtifact(Vertx vertx, JsonObject config, Path basePath, boolean deleteBase, String logConstant) {
        this.vertx = vertx;
        this.config = config;
        this.basePath = basePath;
        this.deleteBase = deleteBase;
        this.logConstant = logConstant;
        this.fileDigestUtil = new FileDigestUtil();
    }

    @Override
    public JsonObject execute(ModuleRequest request) {

        try (FileSystem zipFs = this.getFileSystem(config.getString("artifact.repo") + "/" + request.getFileName())) {

            LOG.info("[{} - {}]: Extracting artifact {} to {}.", logConstant, request.getId(), request.getModuleId(), basePath);
            if (deleteBase) {
                removeBasePath(request, basePath);
            }

            final Path zipRoot = zipFs.getPath("/");

            Files.walkFileTree(zipRoot, CopyingFileVisitor(basePath));
            LOG.info("[{} - {}]: Extracted artifact {} to {}.", LogConstants.DEPLOY_SITE_REQUEST, request.getId(), request.getModuleId(), basePath);
        } catch (IOException | InvalidPathException e) {
            LOG.error("[{} - {}]: Error while extracting artifact {} -> {}.", logConstant, request.getId(), request.getModuleId(), e.getMessage());
            return new JsonObject().putBoolean("success", false);
        }
        return new JsonObject().putBoolean("success", true).putBoolean("configChanged", configChanged);
    }

    private SimpleFileVisitor<Path> CopyingFileVisitor(final Path basePath) {
        return new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (ArtifactContextUtil.ARTIFACT_CONTEXT.equals(file.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                final Path unpackFile = Paths.get(basePath.toString(), file.toString());
                byte[] oldDigest = fileDigestUtil.getFileMd5Sum(unpackFile);
                Files.copy(file, unpackFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                byte[] newDigest = fileDigestUtil.getFileMd5Sum(unpackFile);

                if (!configChanged && !Arrays.equals(oldDigest, newDigest)) {
                    configChanged = true;
                }

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
        };
    }

    private void removeBasePath(ModuleRequest request, Path basePath) {
        if (!basePath.getParent().toFile().exists() || !basePath.getParent().toFile().canWrite()) {
            LOG.warn("[{} - {}]: Unable to extract artifact {} -> {} not exist or not writable.", logConstant, request.getId(), request.getModuleId(), basePath.getParent().toString());
            LOG.warn("[{} - {}]: Unable to extract artifact {} to basePath -> {}.", logConstant, request.getId(), request.getModuleId(), basePath.getParent().toFile().toString());
        }

        if (basePath.toFile().exists()) {
            LOG.info("[{} - {}]: Removing base path -> {}.", logConstant, request.getId(), basePath.toAbsolutePath());
            vertx.fileSystem().deleteSync(basePath.toString(), true);
        }
    }

    private FileSystem getFileSystem(String location) throws IOException {
        Path path = Paths.get(location);
        URI uri = URI.create("jar:file:" + path.toUri().getPath());
        return FileSystems.newFileSystem(uri, new HashMap<String, String>());
    }

}
