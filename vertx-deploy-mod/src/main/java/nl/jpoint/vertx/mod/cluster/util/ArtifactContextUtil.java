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

    public static String getBaseLocation(byte[] metadata ) {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(
                    new ByteArrayInputStream(metadata));
            return (String) xPath.compile(BASE_LOCATION).evaluate(document, XPathConstants.STRING);

        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            return null;
        }
    }
}
