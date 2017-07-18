package nl.jpoint.maven.vertx.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DeployState {
    private static final ObjectWriter writer = new ObjectMapper().writer();

    private String autoscalingGroup;
    private Map<String, DeployRequest> deploymentGroups = new HashMap<>();

    public DeployState(String autoscalingGroup) {
        this.autoscalingGroup = autoscalingGroup;
    }

    public void updateDeploymentGroup(DeployRequest deployRequest) {
        deploymentGroups.put(deployRequest.getDeployGroup(), deployRequest);
    }

    public InputStream inputStream() {


        try (PipedOutputStream pos = new PipedOutputStream();
             PipedInputStream pin = new PipedInputStream()) {
            pin.connect(pos);
            writer.writeValue(pos, this);
            return pin;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
