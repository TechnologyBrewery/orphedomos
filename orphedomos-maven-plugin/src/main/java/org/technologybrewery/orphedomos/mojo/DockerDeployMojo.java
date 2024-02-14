package org.technologybrewery.orphedomos.mojo;

import org.technologybrewery.orphedomos.util.exec.DockerCommandExecutor;
import org.technologybrewery.orphedomos.util.exec.DockerDeployExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@Mojo(name = "push-docker-image", defaultPhase = LifecyclePhase.DEPLOY)
public class DockerDeployMojo extends AbstractDockerMojo {
    private static final Logger logger = LoggerFactory.getLogger(DockerDeployMojo.class);

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        login();
        // TODO: Yucky logic
        if (this.targetArchitectures.length == 0) {
            getDockerDeployInfo(mavenSession.getCurrentProject(), repoUrl).ifPresent((dockerDeployInfo) -> {
                DockerDeployExecutor deployExecutor = new DockerDeployExecutor(dockerContext);
                try {
                    deployExecutor.deploy(dockerDeployInfo, aliases, dryRun);
                } catch(Exception e) {
                    logger.warn(e.getMessage());
                }
            });
        } else {
            // TODO: Handling for manifest unification.  Will address as a supplemental goal most likely.
        }

        logout();
    }
}
