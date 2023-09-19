package org.technologybrewery.orphedomos.util.exec;

import org.technologybrewery.shell.exec.CommandHelper;

import java.io.File;

public class DockerCommandExecutor extends CommandHelper {

    private static final String DOCKER_COMMAND = "docker";

    public DockerCommandExecutor(File workingDirectory) {
        super(workingDirectory, DOCKER_COMMAND);
    }
}
