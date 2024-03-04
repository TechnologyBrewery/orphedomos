package org.technologybrewery.orphedomos.util.exec;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Settings;
import org.technologybrewery.orphedomos.util.OrphedomosException;
import org.technologybrewery.orphedomos.util.credential.CredentialUtil;

public class DockerCredentialExecutor {

    private Settings settings;
    private String repoId;
    private String repoUrl;
    private File dockerContext;
    private boolean usePlainTextPassword;
    private boolean dryRun;

    public DockerCredentialExecutor(Settings settings, String repoId, String repoUrl, File dockerContext, boolean usePlainTextPassword, boolean dryRun) {
        this.settings = settings;
        this.repoId = repoId;
        this.repoUrl = repoUrl;
        this.dockerContext = dockerContext;
        this.usePlainTextPassword = usePlainTextPassword;
        this.dryRun = dryRun;
    }

    private void validateLoginDefined(String username, String password) throws OrphedomosException {
        if (username == null || password == null) {
            throw new OrphedomosException("Missing or invalid credentials for repository id " + repoId + ".  Please validate your Maven settings.xml!");
        }
    }

    public void login() throws MojoExecutionException {
        if (dryRun) { return; }
        String username = CredentialUtil.findUsernameForServer(settings, repoId);
        String password = findPasswordForServer(repoId);

        validateLoginDefined(username, password);

        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        executor.executeWithSensitiveArgsAndLogOutput(Arrays.asList(
                new ImmutablePair<>("login", false),
                new ImmutablePair<>("--username", false),
                new ImmutablePair<>(username, false),
                new ImmutablePair<>("--password", false),
                new ImmutablePair<>(password, true),
                new ImmutablePair<>(repoUrl, false)
        ));
    }

    private String findPasswordForServer(String serverId) throws MojoExecutionException {
        if (dryRun) { return ""; }
        String password;
        if (usePlainTextPassword) {
            password = CredentialUtil.findPlaintextPasswordForServer(settings, serverId);
        } else {
            password = CredentialUtil.decryptServerPassword(settings, serverId);
        }
        return password;
    }

    public void logout() throws MojoExecutionException {
        DockerCommandExecutor executor = new DockerCommandExecutor(dockerContext);
        executor.executeAndLogOutput(Arrays.asList(
                "logout"
        ));
    }
}