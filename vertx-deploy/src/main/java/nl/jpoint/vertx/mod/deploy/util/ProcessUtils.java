package nl.jpoint.vertx.mod.deploy.util;

import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.mod.deploy.Constants;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProcessUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);
    private static final String UUID_PATTERN = "([^\\s]+):([^\\s]+):([^\\s]+)";
    private static final String MAVEN_PATTERN = "maven:([^\\s]+)";

    private final Path vertxHome;
    private final Pattern pattern;

    public ProcessUtils(DeployConfig config) {
        vertxHome = config.getVertxHome();
        pattern = Pattern.compile(MAVEN_PATTERN);
    }

    public Map<String, JsonObject> listInstalledAndRunningModules() {

        List<String> moduleIds = this.listModules();
        return moduleIds.stream()
                .map(this::describeModule)
                .collect(Collectors.toMap((Function<JsonObject, String>) jsonObject -> jsonObject.getString(Constants.MAVEN_ID), Function.identity()));
    }

    private JsonObject describeModule(String applicationId) {
        JsonObject module = new JsonObject();
        String moduleString = "";
        try {
            final Process describeProcess = Runtime.getRuntime().exec(new String[]{"pgrep", "-a", "-f", applicationId});
            describeProcess.waitFor(1, TimeUnit.MINUTES);

            int exitValue = describeProcess.exitValue();
            if (exitValue == 0) {
                BufferedReader out = new BufferedReader(new InputStreamReader(describeProcess.getInputStream()));
                String outLine;
                while ((outLine = out.readLine()) != null) {
                    Matcher matcher = pattern.matcher(outLine);
                    if (matcher.find()) {
                        moduleString = matcher.group(1);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{}]: -  Failed to list modules '{}'", LogConstants.STARTUP, e.getMessage());
        }
        parseModuleString(module, moduleString, applicationId);
        return module;
    }

    private void parseModuleString(JsonObject module, String moduleString, String applicationId) {
        if (moduleString != null && !moduleString.isEmpty()) {
            String[] vars = moduleString.split(":", 3);
            if (vars.length == 3) {
                module.put(Constants.MODULE_VERSION, vars[2]);
                module.put(Constants.MAVEN_ID, vars[0] + ":" + vars[1]);
                module.put(Constants.APPLICATION_ID, applicationId);
            }
        }
    }

    private List<String> listModules() {
        List<String> result = new ArrayList<>();
        try {
            final Process listProcess = Runtime.getRuntime().exec(new String[]{vertxHome.resolve("bin/vertx").toString(), "list"});
            listProcess.waitFor(1, TimeUnit.MINUTES);

            int exitValue = listProcess.exitValue();
            if (exitValue == 0) {
                BufferedReader out = new BufferedReader(new InputStreamReader(listProcess.getInputStream()));
                String outLine;
                while ((outLine = out.readLine()) != null) {
                    if (outLine.matches(UUID_PATTERN) && !result.contains(outLine)) {
                        result.add(outLine);
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            LOG.error("[{}]: -  Failed to list modules '{}'", LogConstants.STARTUP, e.getMessage());
        }
        return result;
    }

    public boolean checkModuleRunning(String moduleId) {
        JsonObject module = describeModule(moduleId);
        return !module.isEmpty();
    }
}
