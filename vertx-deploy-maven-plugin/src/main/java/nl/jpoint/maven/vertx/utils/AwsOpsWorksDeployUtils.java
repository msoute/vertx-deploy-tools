package nl.jpoint.maven.vertx.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.opsworks.AWSOpsWorksClient;
import com.amazonaws.services.opsworks.model.DescribeInstancesRequest;
import com.amazonaws.services.opsworks.model.DescribeInstancesResult;
import nl.jpoint.maven.vertx.config.DeployConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;


public class AwsOpsWorksDeployUtils {

    private final AWSOpsWorksClient awsOpsWorksClient;


    public AwsOpsWorksDeployUtils(Server server, String region) throws MojoFailureException {
        BasicAWSCredentials credentials = new BasicAWSCredentials(server.getUsername(), server.getPassword());
        Region awsRegion = Region.getRegion(Regions.fromName(region));
        awsOpsWorksClient = new AWSOpsWorksClient(credentials);
        awsOpsWorksClient.setRegion(awsRegion);
    }


    public void getHostsOpsWorks(Log log, DeployConfiguration activeConfiguration) throws MojoFailureException {
        log.info("retrieving list of hosts for layer with id : " + activeConfiguration.getOpsWorksLayerId());
        activeConfiguration.getHosts().clear();

        try {
            DescribeInstancesResult result = awsOpsWorksClient.describeInstances(new DescribeInstancesRequest().withLayerId(activeConfiguration.getOpsWorksLayerId()));
            result.getInstances().stream()
                    .filter(i -> i.getStatus().equals("online"))
                    .forEach(i -> {
                                String ip = activeConfiguration.getAwsPrivateIp() ? i.getPrivateIp() : i.getPublicIp();
                                log.info("Adding host from opsworks response : " + ip);
                                activeConfiguration.getHosts().add("http://" + ip + ":6789");
                            }
                    );

        } catch (AmazonClientException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }
}