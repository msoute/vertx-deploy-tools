package nl.jpoint.vertx.mod.cluster.util;

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

public class ArtifactContextUtil {
    private static final XPath xPath = XPathFactory.newInstance().newXPath();

    private static final String BASE_LOCATION = "/artifact/baselocation/text()";
    private static final String RESTART_COMMAND = "/artifact/restartCommand/text()";

    private Document document;

    public ArtifactContextUtil(byte[] data) {
        try {

            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = builderFactory.newDocumentBuilder();
            document = builder.parse(
                    new ByteArrayInputStream(data));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            document = null;
        }
    }

    public String getBaseLocation() {
        try {
            return document != null ? (String) xPath.compile(BASE_LOCATION).evaluate(document, XPathConstants.STRING) : null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public String getRestartCommand() {
        try {
            return document != null ? (String) xPath.compile(RESTART_COMMAND).evaluate(document, XPathConstants.STRING) : null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
