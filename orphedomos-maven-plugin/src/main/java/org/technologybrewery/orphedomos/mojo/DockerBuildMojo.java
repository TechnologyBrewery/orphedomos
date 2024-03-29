package org.technologybrewery.orphedomos.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.orphedomos.util.exec.DockerCommandExecutor;
import org.technologybrewery.orphedomos.util.exec.DockerCredentialExecutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "build-docker-image", defaultPhase = LifecyclePhase.PACKAGE)
public class DockerBuildMojo extends AbstractDockerMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Location of the artifact that will be published for this module.
     */
    @Parameter(property = "orphedomos.mavenArtifactFile", required = true, defaultValue = "${project.basedir}/target/orphedomos.placeholder.txt")
    protected File mavenArtifactFile;

    private static final Logger logger = LoggerFactory.getLogger(DockerBuildMojo.class);

    public void execute() throws MojoExecutionException, MojoFailureException {
        setUpPlaceholderFileAsMavenArtifact();

        super.execute();
    }

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        DockerCredentialExecutor credentials = new DockerCredentialExecutor(settings, repoId, repoUrl, dockerContext, usePlainTextPassword, dryRun);
        if (this.repoId != null && !this.repoId.isBlank())
            credentials.login();

        logger.info("Building docker image...");
        logger.info("Current directory: " + dockerContext);

        if (useBuildKit) {
            this.buildWithBuildKit();
        } else {
            this.buildLegacy();
        }

        applyAliases();

        if (this.repoId != null && !this.repoId.isBlank())
            credentials.logout();
    }

    private void applyAliases() throws MojoExecutionException {
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);

        for (String alias : aliases) {
            executor.executeAndLogOutput(Arrays.asList(
                    "tag",
                    getImageTag(),
                    alias
            ));
        }
    }

    private void buildWithBuildKit() throws MojoExecutionException{
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        List<String> executionArgs = new ArrayList<>(Arrays.asList(
                "buildx",
                "build",
                "--tag",
                getImageTag(),
                "--load",
                "--file",
                getDockerfilePath()
        ));

        executionArgs.addAll(this.getBuildArgumentsList());

        if (buildOptions.size() > 0) {
            executionArgs.addAll(buildOptions);
        }

        executionArgs.add(dockerContext.getAbsolutePath());

        executor.executeAndLogOutput(executionArgs);
    }

    private void buildLegacy() throws MojoExecutionException {
        logger.warn("Building image without BuildKit.  Please consider using BuildKit for enhanced performance!");
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        List<String> executionArgs = new ArrayList<>(Arrays.asList(
                "build",
                "-t",
                getImageTag(),
                "--file",
                getDockerfilePath()
        ));
        executionArgs.addAll(this.getBuildArgumentsList());

        if (buildOptions.size() > 0) {
            executionArgs.addAll(buildOptions);
        }

        executionArgs.add(dockerContext.getAbsolutePath());

        // TODO: What to do if there are target architectures specified?
        // Probably just discard them if we don't have a way to actually manage it but worth examining
        if (targetArchitectures.length == 0) {
            executor.executeAndLogOutput(executionArgs);
        }
    }

    protected void setUpPlaceholderFileAsMavenArtifact() {
        mavenArtifactFile.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(mavenArtifactFile)) {
            writer.println("This is NOT the file you are looking for!");
            writer.println();
            writer.println("To take advantage of the Maven Reactor, we want to publish pom files for this artifact.");
            writer.println("But Maven isn't the right solution for managing Docker images.");
            writer.println();
            writer.println(String.format("Please check your appropriate Docker repository for the %s files instead!",
                    project.getArtifactId()));

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not create placeholder artifact file!", e);
        }

        project.getArtifact().setFile(mavenArtifactFile);
    }
}
