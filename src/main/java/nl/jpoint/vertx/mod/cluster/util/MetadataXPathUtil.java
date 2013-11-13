package nl.jpoint.vertx.mod.cluster.util;


import nl.jpoint.vertx.mod.cluster.request.ModuleRequest;
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

    private static XPath xPath = XPathFactory.newInstance().newXPath();
    private static final String TIMESTAMP = "/metadata/versioning/snapshot/timestamp/text()";
    private static final String BUILD_NUMBER = "/metadata/versioning/snapshot/buildNumber/text()";

    public static String getArtifactIdFromMetadata(byte[] metadata, ModuleRequest request) {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(
                    new ByteArrayInputStream(metadata));
            String timestamp = (String) xPath.compile(TIMESTAMP).evaluate(document, XPathConstants.STRING);
            String buildNumber = (String) xPath.compile(BUILD_NUMBER).evaluate(document, XPathConstants.STRING);

            if (!timestamp.isEmpty() && !buildNumber.isEmpty()) {

                return request.getRemoteLocation(timestamp + "-" + buildNumber);
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            return request.getRemoteLocation();
        }
        return request.getRemoteLocation();
    }
}
