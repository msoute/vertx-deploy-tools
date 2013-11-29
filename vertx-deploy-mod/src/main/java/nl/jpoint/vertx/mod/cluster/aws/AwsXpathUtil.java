package nl.jpoint.vertx.mod.cluster.aws;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
import java.util.ArrayList;
import java.util.List;

public class AwsXpathUtil {
    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    private static final String INSTANCE_LIST = "/DescribeLoadBalancersResponse/DescribeLoadBalancersResult/LoadBalancerDescriptions/member/Instances/member/InstanceId";

    public static List<String> extractInstances(byte[] lbInfo) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        List<String> instances = new ArrayList<>();

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(
                    new ByteArrayInputStream(lbInfo));

            Node instanceNodes = (Node) xPath.compile(INSTANCE_LIST).evaluate(document, XPathConstants.NODE);
            for (int i = 0; i < instanceNodes.getChildNodes().getLength(); i++) {
                Node child = instanceNodes.getChildNodes().item(i);
                instances.add(child.getNodeValue());
            }


        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return instances;
    }
}
