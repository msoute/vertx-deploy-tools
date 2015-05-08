package nl.jpoint.vertx.mod.cluster.aws;

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

    private static final String MEMBERS_LIST = "//member/InstanceId";
    private static final String INSTANCE_STATE = "//member[InstanceId=\"INSTANCE_ID\"]/State/text()";

    private static final String AUTO_SCALING_GROUP_MEMBERS_LIST = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/Instances/member[LifecycleState=\"InService\"]/InstanceId";
    private static final String AUTO_SCALING_GROUP_ELB_LIST = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/LoadBalancerNames/member";
    private static final String AUTO_SCALING_GROUP_INSTANCE_STATE = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/Instances/member[InstanceId=\"${id}\"]/LifecycleState";
    private static final String EC2_PRIVATE_DNS_LIST = "//DescribeInstancesResponse/reservationSet/item/instancesSet/item/privateDnsName";

    public static List<String> listPrivateDNSInDescribeInstancesResponse(byte[] awsResponse) throws AwsException {
        System.out.println(new String(awsResponse));
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        List<String> instances = new ArrayList<>();

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(awsResponse));

            NodeList instanceNodes = (NodeList) xPath.compile(EC2_PRIVATE_DNS_LIST).evaluate(document, XPathConstants.NODESET);


            for (int i = 0; i < instanceNodes.getLength(); i++) {
                Node node = instanceNodes.item(i);
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
            Document document = builder.parse(new ByteArrayInputStream(awsResponse));

            NodeList instanceNodes = (NodeList) xPath.compile(AUTO_SCALING_GROUP_MEMBERS_LIST).evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < instanceNodes.getLength(); i++) {
                instances.add(instanceNodes.item(i).getTextContent().trim());
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return instances;
    }

    public static AwsState getInstanceState(final String instanceId, final byte[] awsResponse) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;

        String instanceState;

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(awsResponse));

            instanceState = (String) xPath.compile(AUTO_SCALING_GROUP_INSTANCE_STATE.replace("${id}", instanceId)).evaluate(document, XPathConstants.STRING);
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return AwsState.valueOf(instanceState.toUpperCase());
    }

    public static List<String> listLoadBalancersInGroup(byte[] awsResponse) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        List<String> elbList = new ArrayList<>();

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(awsResponse));

            NodeList instanceNodes = (NodeList) xPath.compile(AUTO_SCALING_GROUP_ELB_LIST).evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < instanceNodes.getLength(); i++) {
                elbList.add(instanceNodes.item(i).getTextContent().trim());
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return elbList;
    }


    public static String instanceState(byte[] awsResponse, String instanceId) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        String instanceState;

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(awsResponse));
            String dynamicXpath = INSTANCE_STATE.replace("INSTANCE_ID", instanceId);
            instanceState = (String) xPath.compile(dynamicXpath).evaluate(document, XPathConstants.STRING);
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }

        if (instanceState == null || instanceState.isEmpty()) {
            instanceState = AwsState.OUTOFSERVICE.name();
        }

        return instanceState;
    }

    public static List<String> listInstances(byte[] awsResponse) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        List<String> instances = new ArrayList<>();

        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(awsResponse));

            NodeList instanceNodes = (NodeList) xPath.compile(MEMBERS_LIST).evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < instanceNodes.getLength(); i++) {
                instances.add(instanceNodes.item(i).getTextContent());
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
        return instances;
    }
}
