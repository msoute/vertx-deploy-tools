package nl.jpoint.vertx.mod.deploy.util;


import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
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

public class MetadataXPathUtil {

    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    private static final String TIMESTAMP = "/metadata/versioning/snapshot/timestamp/text()";
    private static final String BUILD_NUMBER = "/metadata/versioning/snapshot/buildNumber/text()";

    private static final Logger LOG = LoggerFactory.getLogger(MetadataXPathUtil.class);

    public static String getRealSnapshotVersionFromMetadata(byte[] metadata, ModuleRequest request) {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(
                    new ByteArrayInputStream(metadata));
            String timestamp = (String) xPath.compile(TIMESTAMP).evaluate(document, XPathConstants.STRING);
            String buildNumber = (String) xPath.compile(BUILD_NUMBER).evaluate(document, XPathConstants.STRING);

            if (!timestamp.isEmpty() && !buildNumber.isEmpty() && request.isSnapshot()) {
                return request.getVersion().substring(0, request.getVersion().length() - 8) + timestamp + "-" + buildNumber;
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Error while parsing metadata", e);
            throw new IllegalStateException();
        }
        return null;
    }
}
