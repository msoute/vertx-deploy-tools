package nl.jpoint.maven.vertx.utils;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.*;
import nl.jpoint.maven.vertx.mojo.DeployConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AwsCloudWatchUtils {


    private static final String OK_DEPLOYS = "ok.deploys";
    private static final String FAILED_DEPLOYS = "failed.deploys";
    private static final String AS_GROUP_SIZE = "as.group.size";
    private final AmazonCloudWatch cloudWatch;
    private final int asGroupSize;
    private final DeployConfiguration activeConfiguration;
    private final Log log;

    private Long start = null;

    public AwsCloudWatchUtils(String region, int asGroupSize, DeployConfiguration activeConfiguration, Log log) {
        cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(region).build();
        this.asGroupSize = asGroupSize;
        this.activeConfiguration = activeConfiguration;
        this.log = log;
    }

    public void startTimer() {
        this.start = System.currentTimeMillis();
    }

    public void logSuccess() {
        this.logMetric(OK_DEPLOYS);
    }

    public void logFailed() {
        this.logMetric(FAILED_DEPLOYS);
    }

    private void logMetric(String metricName) {
        if (activeConfiguration.getMetricsConfiguration() == null) {
            return;
        }

        Dimension dimension1 = new Dimension().withName("Application").withValue(activeConfiguration.getMetricsConfiguration().getApplication());
        Dimension dimension2 = new Dimension().withName("Environments").withValue(activeConfiguration.getMetricsConfiguration().getEnvironment());

        List<MetricDatum> datums = new ArrayList<>();


        datums.add(new MetricDatum()
                .withMetricName(AS_GROUP_SIZE)
                .withUnit(StandardUnit.None)
                .withValue((double) asGroupSize));
        datums.add(new MetricDatum()
                .withMetricName(metricName)
                .withUnit(StandardUnit.None)
                .withValue(1.0));

        if (start != null) {
            datums.add(new MetricDatum()
                    .withMetricName("deploy.duration")
                    .withUnit(StandardUnit.Seconds)
                    .withValue((double) Duration.of(System.currentTimeMillis() - start, ChronoUnit.MILLIS).getSeconds()));
        }

        try {
            datums.forEach(metricDatum -> metricDatum.withDimensions(dimension1));
            cloudWatch.putMetricData(new PutMetricDataRequest().withNamespace(activeConfiguration.getMetricsConfiguration().getNamespace()).withMetricData(datums));
            datums.forEach(metricDatum -> metricDatum.withDimensions(dimension2));
            cloudWatch.putMetricData(new PutMetricDataRequest().withNamespace(activeConfiguration.getMetricsConfiguration().getNamespace()).withMetricData(datums));
        } catch (AmazonCloudWatchException e) {
            log.error("Unable to push Cloudwatch metrics.", e);
        }
    }


}
