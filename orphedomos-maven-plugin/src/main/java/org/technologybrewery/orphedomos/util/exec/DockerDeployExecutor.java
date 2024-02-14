package org.technologybrewery.orphedomos.util.exec;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.technologybrewery.orphedomos.util.model.DockerDeployInfo;

public class DockerDeployExecutor {

    private File dockerContext;

    public DockerDeployExecutor(File dockerContext) {
        this.dockerContext = dockerContext;
    }

    public void deploy(DockerDeployInfo deployInfo, String[] aliases, boolean dryRun) throws MojoExecutionException {
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
            executor.executeAndLogOutput(Arrays.asList(
                    "tag",
                    deployInfo.getDockerImageTag(),
                    deployInfo.getDockerDeployImageTag()
            ));
            if (!dryRun) {
                executor.executeAndLogOutput(Arrays.asList(
                        "push",
                        deployInfo.getDockerDeployImageTag()
                ));
            }
            for (String alias : aliases) {
                executor.executeAndLogOutput(Arrays.asList(
                        "tag",
                        deployInfo.getDockerImageTag(),
                        alias
                ));
                executor.executeAndLogOutput(Arrays.asList(
                        "push",
                        alias
                ));
            }
    }
    
}
