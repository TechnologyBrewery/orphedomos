package org.technologybrewery.orphedomos.util.exec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DockerCommandExecutor {

    private static final String DOCKER_COMMAND = "docker";
    private static final Logger logger = LoggerFactory.getLogger(DockerCommandExecutor.class);

    private final File workingDirectory;

    public DockerCommandExecutor(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public int executeWithSensitiveArgsAndLogOutput(List<Pair<String, Boolean>> argAndIsSensitivePairs)
            throws MojoExecutionException {
        if (logger.isInfoEnabled()) {
            List<String> argsWithSensitiveArgsMasked = argAndIsSensitivePairs.stream()
                    .map(pair -> pair.getRight() ? "XXXX" : pair.getLeft()).collect(Collectors.toList());
            logger.info("Executing Docker command: {} {}", DOCKER_COMMAND,
                    StringUtils.join(argsWithSensitiveArgsMasked, " "));
        }
        ProcessExecutor executor = createDockerExecutor(
                argAndIsSensitivePairs.stream().map(Pair::getLeft).collect(Collectors.toList()));
        return executor.executeAndRedirectOutput(logger);
    }

    public int executeAndLogOutput(List<String> arguments) throws MojoExecutionException {
        if (logger.isInfoEnabled()) {
            logger.info("Executing Docker command: {} {}", DOCKER_COMMAND, StringUtils.join(arguments, " "));
        }
        ProcessExecutor executor = createDockerExecutor(arguments);
        return executor.executeAndRedirectOutput(logger);
    }

    protected ProcessExecutor createDockerExecutor(List<String> arguments) {
        List<String> fullCommandArgs = new ArrayList<>();
        fullCommandArgs.add(DOCKER_COMMAND);
        fullCommandArgs.addAll(arguments);
        return new ProcessExecutor(workingDirectory, fullCommandArgs, Platform.guess(), null);
    }
}
