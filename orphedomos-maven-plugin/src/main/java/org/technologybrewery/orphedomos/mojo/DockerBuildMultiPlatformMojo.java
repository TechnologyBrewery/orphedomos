package org.technologybrewery.orphedomos.mojo;

import org.technologybrewery.orphedomos.util.exec.DockerCommandExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "multiplatform-build-deploy")
public class DockerBuildMultiPlatformMojo extends AbstractDockerMojo {
    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!this.isOrphedomosModule()) {
            return;
        }

        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        login();
        List<String> executionArgs = new ArrayList<>(Arrays.asList(
                "buildx",
                "build",
                "--tag",
                getImageTag(),
                "--push",
                "--file",
                getDockerfilePath()
        ));

        if (targetArchitectures.length > 0) {
            executionArgs.add("--platform");
            executionArgs.add(String.join(",", targetArchitectures));
        }

        executionArgs.addAll(getBuildArgumentsList());

        if (buildOptions.size() > 0) {
            executionArgs.addAll(buildOptions);
        }

        executionArgs.add(dockerContext.getAbsolutePath());

        executor.executeAndLogOutput(executionArgs);

        logout();
    }
}
