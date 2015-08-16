package nl.jpoint.vertx.mod.deploy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);

    public static List<Integer> findPidsForModule(final String moduleId, final String logAction, final String requestId) {
        List<Integer> result = new ArrayList<>();
        try {
            final Process pgrep = Runtime.getRuntime().exec(new String[]{"pgrep", "-f", moduleId});
            pgrep.waitFor();

            int exitValue = pgrep.exitValue();

            if (exitValue == 0) {
                BufferedReader output = new BufferedReader(new InputStreamReader(pgrep.getInputStream()));
                String outputLine;
                while ((outputLine = output.readLine()) != null) {
                    String[] spids = outputLine.split(" ");
                    for (String pid : spids) {
                        try {
                            result.add(Integer.parseInt(pid));
                        } catch (NumberFormatException e) {
                            LOG.error("[{} - {}]: Failed to parse pid '{}' for module {}", logAction, requestId, pid, moduleId);
                        }
                    }
                }
            }

            if (exitValue != 0) {
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(pgrep.getErrorStream()));
                String errorLine;
                while ((errorLine = errorOut.readLine()) != null) {
                    LOG.error("[{} - {}]: Listing pids for module {} failed", logAction, requestId, errorLine);
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("[{} - {}]:Listing pids for module {} failed with io exception {}", logAction, requestId, moduleId, e.getMessage());
        }
        return result;
    }

    public static void stopProcesses(final List<Integer> pids, final String moduleId, final String logAction, final UUID id) {
        for (Integer pid : pids) {
            try {
                final Process pgrep = Runtime.getRuntime().exec(new String[]{"kill", "-s", "SIGTERM", pid.toString()});
                pgrep.waitFor();
                int exitValue = pgrep.exitValue();

                if (exitValue == 0) {
                    BufferedReader output = new BufferedReader(new InputStreamReader(pgrep.getInputStream()));
                    String outputLine;
                    while ((outputLine = output.readLine()) != null) {
                        LOG.info("[{} - {}]: Module {} with pid '{}' stopped : {} ", logAction, id, moduleId, pid, outputLine);
                    }
                }
                if (exitValue != 0) {
                    BufferedReader errorOut = new BufferedReader(new InputStreamReader(pgrep.getErrorStream()));
                    String errorLine;
                    while ((errorLine = errorOut.readLine()) != null) {
                        LOG.error("[{} - {}]: Error while stopping module {} with pid {} : ", logAction, id, errorLine);
                    }
                }
                ProcessUtils.removePidFile(new File(moduleId));

            } catch (IOException | InterruptedException e) {
                LOG.error("[{} - {}]: Stopping module {} with pid {} failed with io exception {}", logAction, id, moduleId, pid, e.getMessage());
            }
        }

    }

    private static void removePidFile(File pidFile) {
        if (pidFile.exists() && pidFile.isFile()) {
            pidFile.delete();
        }
    }

    private static void executeCommand(final String[] command, final String logAction, final UUID requestId) {

    }

    public static void writePid(String moduleId) {
        List<Integer> pids = findPidsForModule(moduleId, null, null);
        File pidFile = new File(moduleId.substring(0, moduleId.lastIndexOf('~')) + ".pid");
        try {
            if (pidFile.createNewFile()) {
                FileWriter wrt = new FileWriter(pidFile);
                wrt.write(pids.get(0).toString());
                wrt.flush();
                wrt.close();
            }
        } catch (IOException e) {
            LOG.warn("Error writing pidFile '{}'", e.getMessage());
        }
    }
}
