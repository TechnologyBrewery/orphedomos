package org.technologybrewery.orphedomos.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.technologybrewery.orphedomos.util.credential.CredentialUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractDockerMojo extends AbstractMojo {
    @Parameter(defaultValue="${project.basedir}", property = "orphedomos.docker.context")
    protected File dockerContext;

    @Parameter(defaultValue = "false", property = "orphedomos.build.dryrun")
    protected boolean dryRun;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    @Parameter(defaultValue = "${project.packaging}")
    private String packaging;

    @Parameter(defaultValue = "${project.artifactId}", property = "orphedomos.image.name")
    protected String imageName;

    @Parameter(defaultValue = "${project.version}", property = "orphedomos.image.version")
    protected String imageVersion;

    @Parameter(property = "orphedomos.image.aliases")
    protected String[] aliases;

    @Parameter(defaultValue = "false", property = "orphedomos.credential.usePlainTextPassword")
    protected boolean usePlainTextPassword;

    @Parameter(defaultValue = "", property = "orphedomos.repository.id")
    protected String repoId;

    @Parameter(defaultValue = "", property = "orphedomos.repository.url")
    protected String repoUrl;

    @Parameter(defaultValue = "true", property = "orphedomos.buildkit.enable")
    protected boolean useBuildKit;

    @Parameter(defaultValue = "/src/main/resources/docker/Dockerfile", property = "orphedomos.dockerfile.path")
    protected String dockerfilePath;

    @Parameter(property = "orphedomos.build.architecture")
    protected String[] targetArchitectures;

    @Parameter(property = "orphedomos.build.args")
    protected Map<String, String> buildArgs;

    @Parameter(property = "orphedomos.build.options")
    protected List<String> buildOptions;

    /**
     * Skips all plugin goals, effectively turning them into a no-op
     */
    @Parameter(defaultValue = "false", property = "orphedomos.skip")
    private boolean skip;
    
    @Parameter(defaultValue = "", property = "orphedomos.image.tagsuffix")
    private String tagSuffix;

    protected String getImageTag() {
        return imageName + ":" + imageVersion + (tagSuffix == null ? "" : tagSuffix);
    }

    protected String getImageTagWithArch(String arch) {
        return getImageTag() + "-" + arch;
    }

    protected boolean isOrphedomosModule() {
        return packaging.equalsIgnoreCase("orphedomos");
    }

    protected String getDockerfilePath() {
        return dockerContext.getPath() + (dockerContext.getPath().endsWith(File.separator) ? "" : File.separator) + dockerfilePath;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping execution of all orphedomos-maven-plugin goals as 'skip' is set to true");
        } else {
            doExecute();
        }
    }

    /**
     * Encapsulates all build logic associated with this plugin goal -
     * {@link AbstractDockerMojo#execute()} delegates to this method after
     * performing execution handling that is common across all Mojos.
     *
     * @throws MojoExecutionException a mojo exception
     * @throws MojoFailureException a mojo failure
     */
    abstract protected void doExecute() throws MojoExecutionException, MojoFailureException;
    
    /**
     * Returns a list of all build-time variables with '--build-arg' prepended to each
     */
    protected List<String> getBuildArgumentsList() {
        List<String> args = new ArrayList<>();

        for (Map.Entry<String, String> kv : buildArgs.entrySet()) {
            args.add("--build-arg");
            args.add(kv.getKey() + "=" + kv.getValue());
        }

        return args;
    }

    protected String prependRegistry(String suffix) {
        return repoUrl + (repoUrl.endsWith("/") ? "" : "/") + suffix;
    }

    protected String findPasswordForServer(String serverId) throws MojoExecutionException {
        if (dryRun) { return ""; }
        String password;
        if (usePlainTextPassword) {
            password = CredentialUtil.findPlaintextPasswordForServer(settings, serverId);
        } else {
            password = CredentialUtil.decryptServerPassword(settings, serverId);
        }
        return password;
    }
}
