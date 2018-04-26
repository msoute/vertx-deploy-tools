package nl.jpoint.vertx.deploy.agent.util;

import nl.jpoint.vertx.deploy.agent.request.ModuleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;

public class ArtifactContextUtil {
    public static final String ARTIFACT_CONTEXT = "artifact_context.xml";
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactContextUtil.class);
    private static final String BASE_LOCATION = "/artifact/baselocation/text()";
    private static final String RESTART_ON_CHANGED_CONTENT = "/artifact/checkContent/text()";
    private static final String RESTART_COMMAND = "/artifact/restartCommand/text()";
    private static final String TEST_COMMAND = "/artifact/testCommand/text()";
    private final XPath xPath = XPathFactory.newInstance().newXPath();
    private Document document;

    public ArtifactContextUtil(ModuleRequest request, Path location) {
        byte[] data;
        if (request.getType().equals(ModuleRequest.GZIP_TYPE)) {
            data = new GzipExtractor<>(request).readArtifactContext(location);
        } else {
            try (FileSystem zipFs = this.getFileSystem(location.toString())) {
                Path path = zipFs.getPath(ARTIFACT_CONTEXT);
                data = Files.readAllBytes(path);
            } catch (IOException e) {
                LOG.error("[{} - {}] : No 'artifact_context.xml' in archive. Failing build : {}", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), e.getMessage());
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        try {
            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = builderFactory.newDocumentBuilder();
            document = builder.parse(
                    new ByteArrayInputStream(data));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("[{} - {}] : Corrupt 'artifact_context.xml' in archive. Failing build : {}", LogConstants.DEPLOY_ARTIFACT_REQUEST, request.getId(), e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public String getBaseLocation() {
        try {
            return document != null ? (String) xPath.compile(BASE_LOCATION).evaluate(document, XPathConstants.STRING) : null;
        } catch (XPathExpressionException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean getCheckConfig() {
        try {
            return Boolean.valueOf(document != null ? (String) xPath.compile(RESTART_ON_CHANGED_CONTENT).evaluate(document, XPathConstants.STRING) : null);
        } catch (XPathExpressionException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public String getRestartCommand() {
        try {
            return document != null ? (String) xPath.compile(RESTART_COMMAND).evaluate(document, XPathConstants.STRING) : null;
        } catch (XPathExpressionException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public String getTestCommand() {
        try {
            return document != null ? (String) xPath.compile(TEST_COMMAND).evaluate(document, XPathConstants.STRING) : null;
        } catch (XPathExpressionException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private FileSystem getFileSystem(String location) throws IOException {
        Path path = Paths.get(location);
        URI uri = URI.create("jar:file:" + path.toUri().getPath());
        return FileSystems.newFileSystem(uri, new HashMap<String, String>());
    }
}
