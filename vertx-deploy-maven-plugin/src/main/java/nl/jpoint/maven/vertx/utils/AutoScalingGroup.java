package nl.jpoint.maven.vertx.utils;

import java.util.ArrayList;
import java.util.List;

public class AutoScalingGroup {

    private final List<String> instances;
    private final List<String> elbs;

    private AutoScalingGroup(List<String> instances, List<String> elbs) {
        this.instances = instances;
        this.elbs = elbs;
    }

    public List<String> getInstances() {
        return new ArrayList<>(instances);
    }

    public List<String> getElbs() {
        return new ArrayList<>(elbs);
    }

    public static class Builder {

        private List<String> instances;
        private List<String> elbs;

        public Builder withInstances(List<String> instances) {
            this.instances = instances;
            return this;
        }
        public Builder withElbs(List<String> elbs) {
            this.elbs = elbs;
            return this;
        }

        public AutoScalingGroup build() {
            return new AutoScalingGroup(instances, elbs);
        }
    }
}
