package nl.jpoint.maven.vertx.state;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.CreateOrUpdateTagsRequest;
import com.amazonaws.services.autoscaling.model.Tag;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AsGroupTagsState {

    private static final String LATEST_REQUEST_TAG = "deploy:latest:version";
    private static final String SCOPE_TAG = "deploy:scope:tst";
    private static final String EXCLUSION_TAG = "deploy:exclusions";
    private static final String PROPERTIES_TAGS = "deploy:classifier:properties";
    private static final String AUTO_SCALING_GROUP = "auto-scaling-group";

    private final DeployConfiguration activeConfiguration;
    private final AmazonAutoScaling awsAsClient;

    public AsGroupTagsState(DeployConfiguration activeConfiguration, AmazonAutoScaling awsAsClient) {
        this.activeConfiguration = activeConfiguration;

        this.awsAsClient = awsAsClient;
    }

    public void store(final String version, Properties properties) {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().withPropagateAtLaunch(true)
                .withResourceType(AUTO_SCALING_GROUP)
                .withKey(LATEST_REQUEST_TAG).withValue(version)
                .withResourceId(activeConfiguration.getAutoScalingGroupId()));
        tags.add(new Tag().withPropagateAtLaunch(true)
                .withResourceType(AUTO_SCALING_GROUP)
                .withKey(SCOPE_TAG).withValue(Boolean.toString(activeConfiguration.isTestScope()))
                .withResourceId(activeConfiguration.getAutoScalingGroupId()));

        if (!activeConfiguration.getAutoScalingProperties().isEmpty()) {
            tags.add(new Tag().withPropagateAtLaunch(true)
                    .withResourceType(AUTO_SCALING_GROUP)
                    .withKey(PROPERTIES_TAGS).withValue(activeConfiguration.getAutoScalingProperties().stream().map(key -> key + ":" + getProperty(key, properties)).collect(Collectors.joining(";")))
                    .withResourceId(activeConfiguration.getAutoScalingGroupId())
            );
        }
        if (!activeConfiguration.getExclusions().isEmpty()) {
            tags.add(new Tag().withPropagateAtLaunch(true)
                    .withResourceType(AUTO_SCALING_GROUP)
                    .withKey(EXCLUSION_TAG).withValue(activeConfiguration.getExclusions().stream().map(e -> e.getGroupId() + ":" + e.getGroupId()).collect(Collectors.joining(";")))
                    .withResourceId(activeConfiguration.getAutoScalingGroupId()));
        }
        awsAsClient.createOrUpdateTags(new CreateOrUpdateTagsRequest().withTags(tags));
    }

    private String getProperty(String key, Properties properties) {
        return System.getProperty(key, properties.getProperty(key));
    }

}
