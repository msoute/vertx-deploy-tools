package nl.jpoint.vertx.deploy.agent.util;

import nl.jpoint.vertx.deploy.agent.command.ExtractArtifact;
import nl.jpoint.vertx.deploy.agent.request.ModuleRequest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static nl.jpoint.vertx.deploy.agent.util.ArtifactContextUtil.ARTIFACT_CONTEXT;

public class GzipExtractor<T extends ModuleRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractArtifact.class);

    private final T request;

    public GzipExtractor(T request) {
        this.request = request;
    }

    byte[] readArtifactContext(Path input) {
        try (InputStream in = Files.newInputStream(getDeflatedFile(input));
             TarArchiveInputStream tarIn = new TarArchiveInputStream(in)) {
            ArchiveEntry entry = tarIn.getNextEntry();
            while (entry != null) {
                if (ARTIFACT_CONTEXT.equals(entry.getName())) {
                    byte[] data = new byte[(int) entry.getSize()];
                    tarIn.read(data, 0, data.length);
                    return data;
                }
                entry = tarIn.getNextEntry();
            }
        } catch (IOException e) {
            LOG.warn("[{} - {}]: Error extracting tar  {}.", request.getLogName(), request.getId(), e.getMessage());
            throw new IllegalStateException(e);
        }
        LOG.error("Missing artifact_context.xml in {}", input);
        throw new IllegalStateException("Missing artifact_context.xml in " + input.toString());
    }

    public void extractTar(Path input, Path output) {
        Map<Path, Path> symlinks = new HashMap<>();
        try (InputStream in = Files.newInputStream(getDeflatedFile(input));
             TarArchiveInputStream tarIn = new TarArchiveInputStream(in)) {
            if (!output.toFile().exists()) {
                Files.createDirectory(output);
            }
            TarArchiveEntry entry = (TarArchiveEntry) tarIn.getNextEntry();
            while (entry != null) {
                if (!entry.getName().equals("./")) {
                    if (entry.isSymbolicLink()) {
                        symlinks.put(output.resolve(entry.getName()).toAbsolutePath(), output.resolve(entry.getName()).getParent().resolve(entry.getLinkName()));
                    } else {
                        if (entry.isDirectory()) {
                            Files.createDirectory(output.resolve(entry.getName()));
                        } else if (!ARTIFACT_CONTEXT.equals(entry.getName())) {
                            byte[] contents = new byte[(int) entry.getSize()];
                            tarIn.read(contents, 0, contents.length);
                            Files.write(output.resolve(entry.getName()), contents);
                        }
                    }
                }
                entry = (TarArchiveEntry) tarIn.getNextEntry();
            }
            symlinks.forEach(this::createSymlink);
            Files.delete(getDeflatedFile(input));
        } catch (IOException e) {
            LOG.warn("[{} - {}]: Error extracting tar  {}.", request.getLogName(), request.getId(), e.getMessage());
            throw new IllegalStateException(e);
        }

    }

    public void deflateGz(Path input) {
        File tempFile;
        tempFile = new File(getDeflatedFile(input).toUri());

        try (
                InputStream fin = Files.newInputStream(input);
                BufferedInputStream in = new BufferedInputStream(fin);
                GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
                FileOutputStream out = new FileOutputStream(tempFile)) {

            final byte[] buffer = new byte[4096];
            int n;
            while (-1 != (n = gzIn.read(buffer))) {
                out.write(buffer, 0, n);
            }

        } catch (IOException e) {
            LOG.warn("[{} - {}]: Error extracting gzip  {}.", request.getLogName(), request.getId(), e.getMessage());

            throw new IllegalStateException(e);
        }
    }

    private Path getDeflatedFile(Path input) {
        return input.getParent().resolve(StringUtils.substringBeforeLast(input.getFileName().toString(), ".gz"));
    }

    private void createSymlink(Path link, Path target) {
        try {
            LOG.trace("Creating symbolic link {} -> {}", link, target);
            Files.createSymbolicLink(link, target.toRealPath());
        } catch (IOException e) {
            LOG.warn("[{} - {}]: Error extracting tar  {}.", request.getLogName(), request.getId(), e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
