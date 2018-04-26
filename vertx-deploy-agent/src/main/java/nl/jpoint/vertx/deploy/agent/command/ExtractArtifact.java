package nl.jpoint.vertx.mod.deploy.command;


import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import nl.jpoint.vertx.mod.deploy.util.ArtifactContextUtil;
import nl.jpoint.vertx.mod.deploy.util.FileDigestUtil;
import nl.jpoint.vertx.mod.deploy.util.GzipExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;

import static nl.jpoint.vertx.mod.deploy.request.ModuleRequest.*;
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

        switch (request.getType()) {
            case CONFIG_TYPE:
            case ZIP_TYPE:
                return extractZip(request);
            case GZIP_TYPE:
                return extractGZip(request);
            default:
                LOG.error("Unsupported artifact type : " + request.getType());
                throw new IllegalStateException();
        }
    }


    private Observable<T> extractZip(T request) {
        try (FileSystem zipFs = this.getFileSystem(request.getLocalPath(config.getArtifactRepo()))) {
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

    private Observable<T> extractGZip(T request) {
        GzipExtractor<T> gzipExtractor = new GzipExtractor<>(request);
        if (request.deleteBase()) {
            removeBasePath(request, basePath);
        }
        gzipExtractor.extractTar(request.getLocalPath(config.getArtifactRepo()), basePath);
        return just(request);
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
                if (!subDir.toFile().exists()) {
                    Files.createDirectory(subDir);
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }

    private void removeBasePath(ModuleRequest request, Path basePath) {
        if (!basePath.getParent().toFile().exists() || !basePath.getParent().toFile().canWrite()) {
            LOG.warn("[{} - {}]: Unable to extract artifact {} -> {} not exist or not writable.", request.getLogName(), request.getId(), request.getModuleId(), basePath.getParent());
            LOG.warn("[{} - {}]: Unable to extract artifact {} to basePath -> {}.", request.getLogName(), request.getId(), request.getModuleId(), basePath.getParent().toFile());
        }

        if (basePath.toFile().exists()) {
            LOG.info("[{} - {}]: Removing base path -> {}.", request.getLogName(), request.getId(), basePath.toAbsolutePath());
            vertx.fileSystem().deleteRecursiveBlocking(basePath.toString(), true);
        }
    }

    private FileSystem getFileSystem(Path location) throws IOException {
        URI uri = URI.create("jar:file:" + location.toUri().getPath());
        return FileSystems.newFileSystem(uri, new HashMap<String, String>());
    }

}
