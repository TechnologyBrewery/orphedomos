package org.technologybrewery.orphedomos.util.credential;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.StringUtils;
import org.technologybrewery.orphedomos.util.OrphedomosException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Contains utility functionality for Habushu, including bash script execution
 * and accessing username/password credentials that may be defined within the
 * relevant settings.xml configuration.
 */
public final class CredentialUtil {

    private static final Logger logger = LoggerFactory.getLogger(CredentialUtil.class);

    private CredentialUtil() {
    }

    /**
     * Find the username for a given server in Maven's user settings.
     *
     * @return the username for the server specified in Maven's settings.xml
     */
    public static String findUsernameForServer(Settings settings, String serverId) {
        Server server = settings.getServer(serverId);
        return server != null ? server.getUsername() : null;
    }

    /**
     * Find the plain-text server password, without decryption steps, extracted from Maven's user settings.
     *
     * @return the password for the specified server from Maven's settings.xml
     */
    public static String findPlaintextPasswordForServer(Settings settings, String serverId) {
        Server server = settings.getServer(serverId);
        return server != null ? server.getPassword() : null;
    }

    /**
     * Simple utility method to decrypt a stored password for a server.
     *
     * @param serverId the id of the server to decrypt the password for
     */
    public static String decryptServerPassword(Settings settings, String serverId) {
        String decryptedPassword = null;

        try {
            decryptedPassword = MavenPasswordDecoder.decryptPasswordForServer(settings, serverId);
        } catch (PlexusCipherException | SecDispatcherException e) {
            throw new OrphedomosException("Unable to decrypt stored passwords. Make sure you have a master password " +
                    "set in settings-security.xml and your server password is encrypted with it in settings.xml. See " +
                    "https://maven.apache.org/guides/mini/guide-encryption.html for more details.", e);
        }

        return decryptedPassword;
    }

    /**
     * Run the bash script found at the given location without parameters.
     *
     * @param bashScriptPath absolute path to the bash script
     */
    public static void runBashScript(String bashScriptPath) {
        runBashScript(bashScriptPath, null, true);
    }

    /**
     * Run the bash script found at the given location with the provided parameters.
     *
     * @param bashScriptPath absolute path to the bash script
     * @param parameters     script parameters
     * @param debug          true to log script output as DEBUG, otherwise logged as
     *                       INFO
     */
    public static void runBashScript(String bashScriptPath, String[] parameters, boolean debug) {
        logger.debug("Running bash script located at {}.", bashScriptPath);

        String[] command;
        if (parameters != null && parameters.length > 0) {
            command = new String[parameters.length + 1];

            System.arraycopy(parameters, 0, command, 1, parameters.length);
        } else {
            command = new String[1];
        }

        if (StringUtils.isNotBlank(bashScriptPath)) {
            command[0] = bashScriptPath;

            Process process = null;
            try {
                process = Runtime.getRuntime().exec(command);

                StringBuilder output = new StringBuilder();
                String line;

                BufferedReader stdInReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = stdInReader.readLine()) != null) {
                    output.append(line + "\n");
                }

                BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = stdErrReader.readLine()) != null) {
                    output.append(line + "\n");
                }

                logger.info(output.toString());

                int exitVal = process.waitFor();
                if (exitVal != 0) {
                    throw new OrphedomosException("Error encountered when running bash script located at " + bashScriptPath
                            + "\n    Can run maven build with -X to see the output of the failed script.");
                }

            } catch (IOException | InterruptedException e) {
                throw new OrphedomosException("Could not run bash script.", e);

            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }
    }

    /**
     * Writes a given list of lines to the file located at the provided file path.
     *
     * @param commands the newline-delineated list of String file lines
     * @param filePath the path to the file
     */
    public static void writeLinesToFile(String commands, String filePath) {
        logger.debug("Writing lines to file located at {}.", filePath);

        String noTraversalFilePath = FilenameUtils.getFullPath(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(noTraversalFilePath), Charset.defaultCharset())) {
            writer.write(commands);
        } catch (IOException e) {
            throw new OrphedomosException("Could not write to file.", e);
        }
    }

    /**
     * Creates a new file at the targeted file location and gives full file
     * permissions to the current user.
     *
     * @param newFile the file location
     */
    public static void createFileAndGivePermissions(File newFile) {
        logger.debug("Creating new file at {}.", newFile.getAbsolutePath());

        if (!newFile.exists()) {
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                throw new OrphedomosException("Could not create new file.", e);
            }
        }

        giveFullFilePermissions(newFile.getAbsolutePath());
    }

    /**
     * Gives full read, write, and execute permissions to a file.
     *
     * @param filePath the path to the file
     */
    public static void giveFullFilePermissions(String filePath) {
        File file = new File(FilenameUtils.getName(filePath));

        if (file.exists()) {
            file.setExecutable(true, false);
            file.setReadable(true, false);
            file.setWritable(true, false);
        }
    }

}
