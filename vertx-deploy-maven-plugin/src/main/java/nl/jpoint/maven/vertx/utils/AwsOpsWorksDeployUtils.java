package nl.jpoint.maven.vertx.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.opsworks.AWSOpsWorksClient;
import com.amazonaws.services.opsworks.model.DescribeInstancesRequest;
import com.amazonaws.services.opsworks.model.DescribeInstancesResult;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

@Deprecated
public class AwsOpsWorksDeployUtils {

    public static final String ONLINE = "online";
    private final AWSOpsWorksClient awsOpsWorksClient;


    public AwsOpsWorksDeployUtils(String region) throws MojoFailureException {
        Region awsRegion = Region.getRegion(Regions.fromName(region));
        awsOpsWorksClient = new AWSOpsWorksClient();
        awsOpsWorksClient.setRegion(awsRegion);
    }


    public void getHostsOpsWorks(Log log, DeployConfiguration activeConfiguration) throws MojoFailureException {
        log.info("retrieving list of hosts for layer with id : " + activeConfiguration.getOpsWorksLayerId());
        activeConfiguration.getHosts().clear();

        try {
            DescribeInstancesResult result = awsOpsWorksClient.describeInstances(new DescribeInstancesRequest().withLayerId(activeConfiguration.getOpsWorksLayerId()));
            result.getInstances().stream()
                    .filter(i -> i.getStatus().equals(ONLINE))
                    .forEach(i -> {
                                String ip = activeConfiguration.getAwsPrivateIp() ? i.getPrivateIp() : i.getPublicIp();
                                log.info("Adding host from opsworks response : " + ip);
                                activeConfiguration.getHosts().add("http://" + ip + ":6789");
                            }
                    );

        } catch (AmazonClientException e) {
            log.error(e.getMessage(), e);
            throw new MojoFailureException(e.getMessage());
        }
    }
}