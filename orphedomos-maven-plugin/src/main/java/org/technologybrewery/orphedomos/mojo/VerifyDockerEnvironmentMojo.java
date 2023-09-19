package org.technologybrewery.orphedomos.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.orphedomos.util.OrphedomosException;
import org.technologybrewery.orphedomos.util.exec.DockerCommandExecutor;
import org.technologybrewery.shell.exec.ShellExecutionException;

import java.util.Collections;

@Mojo(name = "verify-docker-environment")
public class VerifyDockerEnvironmentMojo extends AbstractDockerMojo {
    private static final Logger logger = LoggerFactory.getLogger(VerifyDockerEnvironmentMojo.class);

    @Override
    public void doExecute() throws OrphedomosException, MojoExecutionException {
        logger.info("Verifying docker environment...");

        this.verifyDockerInstalled();
        if (useBuildKit) {
            this.verifyBuildKitInstalled();
        }
        this.verifyDockerfileExists();
    }

    private void verifyDockerInstalled() throws OrphedomosException {
        logger.info("Verifying docker command available...");
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        try {
            executor.executeAndLogOutput(Collections.singletonList("--version"));
            logger.info("docker command found successfully.");
        } catch(ShellExecutionException mex) {
            String msg = "docker command was not found!  Please verify that the docker executable is installed and " +
                    "available on your PATH";
            logger.error(msg);
            throw new OrphedomosException(msg, mex);
        }
    }

    private void verifyBuildKitInstalled() throws OrphedomosException, MojoExecutionException {
        logger.info("Verifying buildkit installed and available...");
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        try {
            executor.executeAndLogOutput(Collections.singletonList("buildx"));
            logger.info("docker buildkit found successfully.");
        } catch(OrphedomosException oex) {
            String msg = "BuildKit was requested but not detected.  If you do not wish to install BuildKit, please " +
                    "opt out with either: \n" +
                    "\tpom.xml:\n" +
                    "\t<useBuildKit>false</useBuildKit>\n" +
                    "or\n" +
                    "\tCLI:\n" +
                    "\t-Dorphedomos.buildkit.enable=false";
            logger.error(msg);
            throw new OrphedomosException(msg);
        }
    }

    private void verifyDockerfileExists() throws OrphedomosException {
        if(!this.dockerContext.exists()) {
            String errorText = "Dockerfile not found at " + this.dockerContext.getAbsolutePath();
            logger.error(errorText);
            throw new OrphedomosException(errorText);
        }
        logger.info("Target Dockerfile found at " + this.dockerContext.getPath());
    }
}
