package nl.jpoint.maven.vertx.utils;

import java.util.ArrayList;
import java.util.List;

public class AutoScalingGroup {

    private final List<String> instances;
    private final List<String> elbs;
    private final Integer maxInstances;
    private final Integer minInstances;
    private final Integer desiredCapacity;

    private AutoScalingGroup(List<String> instances, List<String> elbs, Integer minInstances, Integer maxInstances, Integer desiredCapacity) {
        this.instances = instances;
        this.elbs = elbs;
        this.maxInstances = maxInstances;
        this.minInstances = minInstances;
        this.desiredCapacity = desiredCapacity;
    }

    public List<String> getInstances() {
        return new ArrayList<>(instances);
    }

    public List<String> getElbs() {
        return new ArrayList<>(elbs);
    }

    public boolean deployable() {
        return !(minInstances.equals(1) && desiredCapacity.equals(1) && instances.size() <= 1);
    }

    public static class Builder {

        private List<String> instances;
        private List<String> elbs;

        private Integer maxInstances = 0;
        private Integer minInstances = 0;
        private Integer desiredCapacity = 0;

        public Builder withInstances(List<String> instances) {
            this.instances = instances;
            return this;
        }

        public Builder withElbs(List<String> elbs) {
            this.elbs = elbs;
            return this;
        }

        public Builder withMaxInstances(int maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        public Builder withMinInstances(int minInstances) {
            this.minInstances = minInstances;
            return this;
        }

        public Builder withDesiredCapacity(int desiredCapacity) {
            this.desiredCapacity = desiredCapacity;
            return this;
        }

        public AutoScalingGroup build() {
            return new AutoScalingGroup(instances, elbs, minInstances, maxInstances, desiredCapacity);
        }


    }
}
