package org.technologybrewery.orphedomos.mojo;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.technologybrewery.orphedomos.util.OrphedomosException;
import org.technologybrewery.orphedomos.util.credential.CredentialUtil;
import org.technologybrewery.orphedomos.util.exec.DockerCommandExecutor;
import org.technologybrewery.orphedomos.util.model.DockerDeployInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public abstract class AbstractDockerMojo extends AbstractMojo {

    private static String DOCKER_IMAGE_NAME = "dockerImageName";
    private static String ORPHEDOMOS_PACKAGING = "orphedomos";
    private static String TAG_SUFFIX = "orphedomos.image.tagsuffix";

    @Parameter(required = true, readonly = true, defaultValue = "${session}")
    MavenSession mavenSession;

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

    /**
     * Skips all plugin goals, effectively turning them into a no-op
     */
    @Parameter(defaultValue = "false", property = "orphedomos.skip")
    private boolean skip;
    
    @Parameter(defaultValue = "", property = "orphedomos.image.tagsuffix")
    private String tagSuffix;

    protected String getImageTag() {
        return getImageTag(imageName, imageVersion, tagSuffix);
    }

    protected static String getImageTag(String imageName, String imageVersion) {
        return getImageTag(imageName, imageVersion, null);
    }

    protected static String getImageTag(String imageName, String imageVersion, String tagSuffix) {
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
    
    protected List<String> getArgumentsList() {
        List<String> args = new ArrayList<>();

        for (Map.Entry<String, String> kv : buildArgs.entrySet()) {
            args.add("--build-arg");
            args.add(kv.getKey() + "=" + kv.getValue());
        }

        return args;
    }

    private void validateLoginDefined(String username, String password) throws OrphedomosException {
        if (username == null || password == null) {
            throw new OrphedomosException("Missing or invalid credentials for repository id " + repoId + ".  Please validate your Maven settings.xml!");
        }
    }

    protected void login() throws MojoExecutionException {
        if (dryRun) { return; }
        String username = CredentialUtil.findUsernameForServer(settings, repoId);
        String password = findPasswordForServer(repoId);

        validateLoginDefined(username, password);

        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        executor.executeWithSensitiveArgsAndLogOutput(Arrays.asList(
                new ImmutablePair<>("login", false),
                new ImmutablePair<>("--username", false),
                new ImmutablePair<>(username, false),
                new ImmutablePair<>("--password", false),
                new ImmutablePair<>(password, true),
                new ImmutablePair<>(repoUrl, false)
        ));
    }

    protected void logout() throws MojoExecutionException {
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        executor.executeAndLogOutput(Arrays.asList(
                "logout"
        ));
    }

    protected static String prependRegistry(String repoUrl, String suffix) {
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

    protected static Optional<DockerDeployInfo> getDockerDeployInfo(MavenProject project, String repoUrl) {
        String packaging = project.getPackaging();
        if(!packaging.equalsIgnoreCase(ORPHEDOMOS_PACKAGING)) {
            return Optional.empty();
        }
        Properties properties = project.getProperties();
        String dockerImageName = properties.getProperty(DOCKER_IMAGE_NAME);
        String imageTag;
        if(dockerImageName != null) {
            imageTag = getImageTag(dockerImageName, project.getVersion());
        }
        else {
            imageTag = getImageTag(project.getArtifactId(), project.getVersion(), properties.getProperty(TAG_SUFFIX));
        }
        String deployImageTag = prependRegistry(repoUrl, imageTag);
        return Optional.of(new DockerDeployInfo(imageTag, deployImageTag));
    }

}
