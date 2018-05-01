package nl.jpoint.vertx.deploy.agent.util;

import io.vertx.core.json.JsonObject;
import nl.jpoint.vertx.deploy.agent.Constants;
import nl.jpoint.vertx.deploy.agent.DeployConfig;
import nl.jpoint.vertx.deploy.agent.request.DeployApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static rx.Observable.just;

public class ProcessUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);


    private static final String SELF = "nl.jpoint.vertx-deploy-tools:vertx-deploy-agent:";
    private static final String MAVEN_PATTERN = "maven:([^\\s]+)";
    private static final String MODULE_PATTERN = "([^\\s]+):([^\\s]+):([^\\s]+)";
    private final Path vertxHome;
    private final Pattern mavenPattern;
    private final Pattern modulePattern;


    public ProcessUtils(DeployConfig config) {
        vertxHome = config.getVertxHome();
        mavenPattern = Pattern.compile(MAVEN_PATTERN);
        modulePattern = Pattern.compile(MODULE_PATTERN);
    }

    public Map<String, String> listInstalledAndRunningModules() {
        List<String> moduleIds = this.listModules();
        return moduleIds.stream()
                .map(this::parseModuleString)
                .collect(Collectors.toMap(jsonObject -> jsonObject.getString(Constants.MAVEN_ID),
                        jsonObject -> jsonObject.getString(Constants.MODULE_VERSION)));
    }

    private JsonObject parseModuleString(String moduleString) {
        JsonObject module = new JsonObject();
        if (moduleString != null && !moduleString.isEmpty()) {
            String[] vars = moduleString.split(":", 3);
            if (vars.length == 3) {
                module.put(Constants.MODULE_VERSION, vars[2]);
                module.put(Constants.MAVEN_ID, vars[0] + ":" + vars[1]);
            }
        }

        return module;
    }

    public List<String> listModules() {
        List<String> result = new ArrayList<>();
        try {
            final Process listProcess = Runtime.getRuntime().exec(new String[]{vertxHome.resolve("bin/vertx").toString(), "list"});
            listProcess.waitFor(1, TimeUnit.MINUTES);
            int exitValue = listProcess.exitValue();
            if (exitValue == 0) {
                BufferedReader out = new BufferedReader(new InputStreamReader(listProcess.getInputStream()));
                String outLine;
                while ((outLine = out.readLine()) != null) {
                    Matcher mavenMatcher = mavenPattern.matcher(outLine);
                    if (mavenMatcher.find()) {
                        String moduleString = mavenMatcher.group(1);
                        if (moduleString.contains(":") && !result.contains(moduleString) && !moduleString.contains(SELF)) {
                            result.add(moduleString);
                            continue;
                        }
                    }
                    Matcher moduleMatcher = modulePattern.matcher(outLine);
                    if (moduleMatcher.find()) {
                        String moduleString = moduleMatcher.group(0);
                        if (moduleString.contains(":") && !result.contains(moduleString) && !moduleString.contains(SELF)) {
                            result.add(moduleString);
                        }
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{}]: -  Failed to list modules '{}'", LogConstants.STARTUP, e.getMessage(), e);
        }
        return result;
    }

    public Observable<DeployApplicationRequest> checkModuleRunning(DeployApplicationRequest request) {
        Map<String, String> installedModules = listInstalledAndRunningModules();
        request.setRunning(installedModules.containsKey(request.getMavenArtifactId()));
        request.setInstalled(installedModules.containsKey(request.getMavenArtifactId()) && installedModules.get(request.getMavenArtifactId()).equals(request.getVersion()));
        return just(request);
    }

    public String getRunningVersion(DeployApplicationRequest request) {
        return listInstalledAndRunningModules().get(request.getMavenArtifactId());
    }

    public static boolean killService(String service) {
        ProcessBuilder processKillBuilder = new ProcessBuilder().command(Arrays.asList("/usr/bin/pgrep", "-f", service, "|", "xargs", "kill", "-9"));
        try {
            processKillBuilder.start();
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return false;
        }
    }
}
