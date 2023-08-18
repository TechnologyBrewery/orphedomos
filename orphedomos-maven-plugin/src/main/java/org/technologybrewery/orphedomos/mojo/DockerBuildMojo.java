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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "build-docker-image", defaultPhase = LifecyclePhase.PACKAGE)
public class DockerBuildMojo extends AbstractDockerMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    private static final Logger logger = LoggerFactory.getLogger(DockerBuildMojo.class);

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (this.repoId != null && !this.repoId.isBlank())
            login();

        logger.info("Built docker image...");
        logger.info("Current directory: " + dockerContext);

        if (useBuildKit) {
            this.buildWithBuildKit();
        } else {
            this.buildLegacy();
        }

        applyAliases();

        if (this.repoId != null && !this.repoId.isBlank())
            logout();

        attachDockerfileArtifact();
    }

    private void attachDockerfileArtifact() {
        project.getArtifact().setFile(new File(getDockerfilePath()));
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

        executionArgs.addAll(this.getArgumentsList());

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
        executionArgs.addAll(this.getArgumentsList());

        executionArgs.add(dockerContext.getAbsolutePath());

        // TODO: What to do if there are target architectures specified?
        // Probably just discard them if we don't have a way to actually manage it but worth examining
        if (targetArchitectures.length == 0) {
            executor.executeAndLogOutput(executionArgs);
        }
    }
}
