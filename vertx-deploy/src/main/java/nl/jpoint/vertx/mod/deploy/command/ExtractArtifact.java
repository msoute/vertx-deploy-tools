package nl.jpoint.vertx.mod.deploy.command;


import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.ArtifactContextUtil;
import nl.jpoint.vertx.mod.deploy.util.FileDigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;

import static rx.Observable.just;

public class ExtractArtifact<T extends ModuleRequest> implements Command<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractArtifact.class);

    private final Vertx vertx;
    private final DeployConfig config;
    private final Path basePath;
    private final FileDigestUtil fileDigestUtil;

    public ExtractArtifact(io.vertx.core.Vertx vertx, DeployConfig config, Path basePath) {
        this.vertx = new Vertx(vertx);
        this.config = config;
        this.basePath = basePath;
        this.fileDigestUtil = new FileDigestUtil();
    }

    @Override
    public Observable<T> executeAsync(T request) {
        try (FileSystem zipFs = this.getFileSystem(config.getArtifactRepo() + "/" + request.getFileName())) {
            LOG.info("[{} - {}]: Extracting artifact {} to {}.", request.getLogName(), request.getId(), request.getModuleId(), basePath);
            if (request.deleteBase()) {
                removeBasePath(request, basePath);
            }
            final Path zipRoot = zipFs.getPath("/");
            Files.walkFileTree(zipRoot, copyingFileVisitor(basePath, request));
            LOG.info("[{} - {}]: Extracted artifact {} to {}.", request.getLogName(), request.getId(), request.getModuleId(), basePath);
            return just(request)
                    .doOnError(t -> LOG.error("Unable to extract artifact {}, {}", t.getMessage(), t));
        } catch (IOException | InvalidPathException e) {
            LOG.error("[{} - {}]: Error while extracting artifact {} -> {}.", request.getLogName(), request.getId(), request.getModuleId(), e.getMessage(), e);
            throw new IllegalStateException();
        }
    }

    private SimpleFileVisitor<Path> copyingFileVisitor(final Path basePath, T request) {
        return new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                byte[] oldDigest = null;
                byte[] newDigest = null;
                if (ArtifactContextUtil.ARTIFACT_CONTEXT.equals(file.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                final Path unpackFile = Paths.get(basePath.toString(), file.toString());
                if (request.checkConfig()) {
                    oldDigest = fileDigestUtil.getFileMd5Sum(unpackFile);
                }
                Files.copy(file, unpackFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                if (request.checkConfig()) {
                    newDigest = fileDigestUtil.getFileMd5Sum(unpackFile);
                }
                if (!request.restart() && request.checkConfig() && !Arrays.equals(oldDigest, newDigest)) {
                    LOG.warn("[{} - {}]: Config changed, forcing container restart if necessary.", request.getLogName(), request.getId(), request.getModuleId());
                    request.setRestart(true);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
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
            LOG.warn("[{} - {}]: Unable to extract artifact {} -> {} not exist or not writable.", request.getLogName(), request.getId(), request.getModuleId(), basePath.getParent().toString());
            LOG.warn("[{} - {}]: Unable to extract artifact {} to basePath -> {}.", request.getLogName(), request.getId(), request.getModuleId(), basePath.getParent().toFile().toString());
        }

        if (basePath.toFile().exists()) {
            LOG.info("[{} - {}]: Removing base path -> {}.", request.getLogName(), request.getId(), basePath.toAbsolutePath());
            vertx.fileSystem().deleteRecursiveBlocking(basePath.toString(), true);
        }
    }

    private FileSystem getFileSystem(String location) throws IOException {
        Path path = Paths.get(location);
        URI uri = URI.create("jar:file:" + path.toUri().getPath());
        return FileSystems.newFileSystem(uri, new HashMap<String, String>());
    }

}
