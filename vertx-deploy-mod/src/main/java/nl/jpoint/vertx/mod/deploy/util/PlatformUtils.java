package nl.jpoint.vertx.mod.deploy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PlatformUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PlatformUtils.class);

    private static final String CONF_REPOS_TXT = "/conf/repos.txt";

    public static List<String> initializeRepoList(String requestType, String vertxHome) {
        List<String> remoteRepositories = new ArrayList<>();
        String reposFile = vertxHome + CONF_REPOS_TXT;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(reposFile))));

            String line;
            remoteRepositories = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("maven:")) {
                    remoteRepositories.add(line.substring(6));
                }
            }
        } catch (IOException e) {
            LOG.error("[{}]: Error initializing remote repositories {}.", requestType, e.getMessage());
        }
        if (remoteRepositories.size() == 0) {
            LOG.error("[{}]: No remote repositories initialized {}.", requestType);
        }

        return remoteRepositories;
    }
}
