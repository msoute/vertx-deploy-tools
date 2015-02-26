package nl.jpoint.maven.vertx.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

    private static final String AUTO_SCALING_GROUP_MEMBERS_LIST = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/Instances/member[LifecycleState=\"InService\"]/InstanceId";
    private static final String EC2_PRIVATE_DNS_LIST = "//DescribeInstancesResponse/reservationSet/item/instancesSet/item/privateDnsName";
    private static final String EC2_INSTANCE_LIST = "//DescribeInstancesResponse/reservationSet/item/instancesSet/item";

    public static List<String> listPrivateDNSInDescribeInstancesResponse(byte[] awsResponse) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        List<String> instances = new ArrayList<>();

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse( new ByteArrayInputStream(awsResponse));

            NodeList instanceNodes =  (NodeList)xPath.compile(EC2_PRIVATE_DNS_LIST).evaluate(document, XPathConstants.NODESET);

            for  (int i = 0; i < instanceNodes.getLength();i++) {
                instances.add(instanceNodes.item(i).getTextContent());
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return instances;
    }
    public static List<String> listInstancesInAutoscalingGroupResponse(byte[] awsResponse) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        List<String> instances = new ArrayList<>();

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse( new ByteArrayInputStream(awsResponse));

            NodeList instanceNodes =  (NodeList)xPath.compile(AUTO_SCALING_GROUP_MEMBERS_LIST).evaluate(document, XPathConstants.NODESET);


            for  (int i = 0; i < instanceNodes.getLength();i++) {
                instances.add(instanceNodes.item(i).getTextContent());
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return instances;
    }

    public static List<Ec2Instance> describeInstances(byte[] awsResponse) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        List<Ec2Instance> instances = new ArrayList<>();

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse( new ByteArrayInputStream(awsResponse));

            NodeList instanceNodes =  (NodeList)xPath.compile(EC2_INSTANCE_LIST).evaluate(document, XPathConstants.NODESET);

            for  (int i = 0; i < instanceNodes.getLength();i++) {
                Node node =instanceNodes.item(i);
                String instanceId = (String) xPath.compile("//instanceId").evaluate(node, XPathConstants.STRING);
                String privateIp = (String) xPath.compile("//privateIpAddress").evaluate(node, XPathConstants.STRING);
                String publicIp = (String) xPath.compile("//ipAddress").evaluate(node, XPathConstants.STRING);

                instances.add(new Ec2Instance.Builder()
                        .withInstanceId(instanceId)
                        .withPrivateIp(privateIp)
                        .withPublicIp(publicIp)
                        .build()
                );
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return instances;
    }
}
