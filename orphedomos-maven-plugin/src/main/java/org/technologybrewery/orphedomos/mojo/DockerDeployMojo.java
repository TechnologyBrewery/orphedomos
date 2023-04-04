package org.technologybrewery.orphedomos.mojo;

import org.technologybrewery.orphedomos.util.exec.DockerCommandExecutor;
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
                        prependRegistry(alias)
                ));
                executor.executeAndLogOutput(Arrays.asList(
                        "push",
                        prependRegistry(alias)
                ));
//                executor.executeAndLogOutput(Arrays.asList(
//                        "manifest",
//                        "create",
//                        prependRegistry(alias),
//                        prependRegistry(getImageTag()),
//                        "--amend"
//                ));
//                if (!dryRun) {
//                    executor.executeAndLogOutput(Arrays.asList(
//                            "manifest",
//                            "push",
//                            prependRegistry(alias),
//                            "--purge"
//                    ));
//                }
            }
        } else {
            // TODO: Handling for manifest unification
//            List<String> manifestCmdArgs = new ArrayList<>(Arrays.asList(
//                    "manifest",
//                    "create",
//                    getImageTag()
//            ));
//            for (String arch : targetArchitectures) {
//                executor.executeAndLogOutput(Arrays.asList(
//                        "push",
//                        getImageTagWithArch(arch)
//                ));
//
//                manifestCmdArgs.add("--amend");
//                manifestCmdArgs.add(getImageTagWithArch(arch));
//            }
//
//            executor.executeAndLogOutput(manifestCmdArgs);
//
//            executor.executeAndLogOutput(Arrays.asList(
//                    "manifest",
//                    "push",
//                    getImageTag()
//            ));
        }

        logout();
    }
}
