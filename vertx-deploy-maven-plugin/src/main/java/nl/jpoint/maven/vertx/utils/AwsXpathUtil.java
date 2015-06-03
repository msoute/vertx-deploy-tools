package nl.jpoint.maven.vertx.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    private static final String AUTO_SCALING_GROUP_MEMBERS_LIST_INCLUDE_INSTANDBY = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/Instances/member[LifecycleState=\"InService\" or LifecycleState=\"Standby\"]/InstanceId";
    private static final String AUTO_SCALING_GROUP_MIN_INSTACES = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/MinSize";
    private static final String AUTO_SCALING_GROUP_MAX_INSTACES = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/MaxSize";
    private static final String AUTO_SCALING_GROUP_DESIRED_CAPACITY = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/DesiredCapacity";
    private static final String AUTO_SCALING_GROUP_ELB_LIST = "//DescribeAutoScalingGroupsResponse/DescribeAutoScalingGroupsResult/AutoScalingGroups/member/LoadBalancerNames/member";
    private static final String EC2_PRIVATE_DNS_LIST = "//DescribeInstancesResponse/reservationSet/item/instancesSet/item/privateDnsName";
    private static final String EC2_INSTANCE_LIST = "//DescribeInstancesResponse/reservationSet/item";
    private static final String ELB_MEMBER_LIST = "//DescribeInstanceHealthResponse/DescribeInstanceHealthResult";

    public static List<String> listPrivateDNSInDescribeInstancesResponse(byte[] awsResponse) throws AwsException {
        List<String> instances = new ArrayList<>();
        NodeList instanceNodes = listNodes(awsResponse, EC2_PRIVATE_DNS_LIST);
        for (int i = 0; i < instanceNodes.getLength(); i++) {
            instances.add(instanceNodes.item(i).getTextContent());
        }
        return instances;
    }

    public static List<String> listInstancesInAutoscalingGroupResponse(byte[] awsResponse, boolean ignoreInStandby) throws AwsException {
        return listStringItems(awsResponse,  ignoreInStandby ? AUTO_SCALING_GROUP_MEMBERS_LIST : AUTO_SCALING_GROUP_MEMBERS_LIST_INCLUDE_INSTANDBY);
    }

    public static List<String> listELBsInAutoscalingGroupResponse(byte[] awsResponse) throws AwsException {
        return listStringItems(awsResponse, AUTO_SCALING_GROUP_ELB_LIST);
    }

    public static int listMinimalInstancesInAutoscalingGroupResponse(byte[] result) throws AwsException {
        return Integer.valueOf(listStringItem(result, AUTO_SCALING_GROUP_MIN_INSTACES));
    }
    public static int listDesiredCapacityInAutoscalingGroupResponse(byte[] result) throws AwsException {
        return Integer.valueOf(listStringItem(result, AUTO_SCALING_GROUP_DESIRED_CAPACITY));
    }

    public static int listMaximumInstancesInAutoscalingGroupResponse(byte[] result) throws AwsException {
        return Integer.valueOf(listStringItem(result, AUTO_SCALING_GROUP_MAX_INSTACES));
    }


    public static void updateInstanceState(List<Ec2Instance> instances, byte[] awsResponse) throws AwsException {
        Node node = getNode(awsResponse, "//DescribeInstanceHealthResponse/DescribeInstanceHealthResult/InstanceStates");
        for (Ec2Instance instance : instances) {
            String state = getElementValueAsString((Element) node, "member[InstanceId=\"" + instance.getInstanceId() + "\"]/State");
            instance.updateState(AwsState.valueOf(state != null ? state.toUpperCase() : AwsState.OUTOFSERVICE.name()));
        }

    }


    public static List<Ec2Instance> describeInstances(byte[] awsResponse, String expectedTag, List<String> instanceIds) throws AwsException {
        List<Ec2Instance> instances = new ArrayList<>();
        NodeList instanceNodes = listNodes(awsResponse, EC2_INSTANCE_LIST);
        for (int i = 0; i < instanceNodes.getLength(); i++) {
            String instanceId = getElementValueAsString((Element) instanceNodes.item(i), "instancesSet/item/instanceId");
            String privateIp = getElementValueAsString((Element) instanceNodes.item(i), "instancesSet/item/privateIpAddress");
            String publicIp = getElementValueAsString((Element) instanceNodes.item(i), "instancesSet/item/ipAddress");
            if (expectedTag != null) {
                String tag = getElementValueAsString((Element) instanceNodes.item(i), "instancesSet/item/tagSet/item[key=\"Name\"]/value");
                if (!expectedTag.equals(tag)) {
                    throw new AwsException("Expecting tag " + expectedTag + ", got tag.");
                }
            }
            if (!instanceIds.contains(instanceId)) {
                throw new AwsException("Expecting instanceId " + instanceId + " to be a member of requested instanceIds. It is not ! ");
            }
            instances.add(new Ec2Instance.Builder()
                            .withInstanceId(instanceId)
                            .withPrivateIp(privateIp)
                            .withPublicIp(publicIp)
                            .build()
            );
        }

        return instances;
    }


    private static String getElementValueAsString(Element element, String xpath) throws AwsException {
        if (element == null) {
            return null;
        }
        try {
            return (String) xPath.compile(xpath).evaluate(element, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new AwsException(e);
        }
    }

    private static String listStringItem(byte[] awsResponse, String xpath) throws AwsException {
        try {
            Document document = getDocument(awsResponse);
            return (String) xPath.compile(xpath).evaluate(document, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new AwsException(e);
        }
    }

    private static List<String> listStringItems(byte[] awsResponse, String xpath) throws AwsException {
        List<String> items = new ArrayList<>();
        try {
            Document document = getDocument(awsResponse);
            NodeList instanceNodes = (NodeList) xPath.compile(xpath).evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < instanceNodes.getLength(); i++) {
                items.add(instanceNodes.item(i).getTextContent());
            }
        } catch (XPathExpressionException e) {
            throw new AwsException(e);
        }
        return items;
    }

    private static NodeList listNodes(byte[] awsResponse, String xpath) throws AwsException {
        try {
            Document document = getDocument(awsResponse);
            return (NodeList) xPath.compile(xpath).evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new AwsException(e);
        }

    }

    private static Document getDocument(byte[] awsResponse) throws AwsException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = builderFactory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(awsResponse));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new AwsException(e);
        }
    }

    private static Node getNode(byte[] awsResponse, String xpath) throws AwsException {
        try {
            Document document = getDocument(awsResponse);
            return (Node) xPath.compile(xpath).evaluate(document, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new AwsException(e);
        }

    }


}
