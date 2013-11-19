package nl.jpoint.maven.vertx.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "deploy-site")
public class VertxDeploySiteMojo extends AbstractMojo {

    private static final String SITE_CLASSIFIER = "site";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

    }

}
