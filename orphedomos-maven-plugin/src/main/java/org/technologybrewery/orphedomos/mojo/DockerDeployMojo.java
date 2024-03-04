package org.technologybrewery.orphedomos.mojo;

import org.technologybrewery.orphedomos.util.exec.DockerCommandExecutor;
import org.technologybrewery.orphedomos.util.exec.DockerCredentialExecutor;
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
        if (!this.isOrphedomosModule()) {
            return;
        }
        DockerCredentialExecutor credentials = new DockerCredentialExecutor(settings, repoId, repoUrl, dockerContext, usePlainTextPassword, dryRun);
        credentials.login();

        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        // TODO: Yucky logic
        if (this.targetArchitectures.length == 0) {
            String newName = prependRegistry(getImageTag());
            executor.executeAndLogOutput(Arrays.asList(
                    "tag",
                    getImageTag(),
                    newName
            ));
            if (!dryRun) {
                executor.executeAndLogOutput(Arrays.asList(
                        "push",
                        newName
                ));
            }
            for (String alias : aliases) {
                executor.executeAndLogOutput(Arrays.asList(
                        "tag",
                        getImageTag(),
                        alias
                ));
                executor.executeAndLogOutput(Arrays.asList(
                        "push",
                        alias
                ));
            }
        } else {
            // TODO: Handling for manifest unification.  Will address as a supplemental goal most likely.
        }

        credentials.logout();
    }
}
