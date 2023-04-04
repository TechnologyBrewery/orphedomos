package org.technologybrewery.orphedomos.util.credential;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

import java.io.File;
import java.util.List;

/**
 * A simple Maven password decoding tool adapted from open-source code.
 */
public class MavenPasswordDecoder {

    private MavenPasswordDecoder() {
    }

    /**
     * The settings-security.xml file for the current Maven user.
     */
    private static final File ORIGINAL_SETTINGS_SECURITY_FILE = new File(System.getProperty("user.home"),
            ".m2/settings-security.xml");

    private static String decodePassword(String encodedPassword, String key) throws PlexusCipherException {
        DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        return cipher.decryptDecorated(encodedPassword, key);
    }

    private static String decodeMasterPassword(String encodedMasterPassword) throws PlexusCipherException {
        return decodePassword(encodedMasterPassword, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
    }

    private static SettingsSecurity readSettingsSecurity(File file) throws SecDispatcherException {
        return SecUtil.read(file.getAbsolutePath(), true);
    }

    public static String decryptPasswordForServer(Settings settings, String serverId)
            throws SecDispatcherException, PlexusCipherException {

        SettingsSecurity settingsSecurity = null;

        if (System.getProperty("settings.security") != null) {
            File movedSettingsSecurityFile = new File(System.getProperty("settings.security"));
            settingsSecurity = readSettingsSecurity(movedSettingsSecurityFile);
        } else if (ORIGINAL_SETTINGS_SECURITY_FILE.exists()) {
            settingsSecurity = readSettingsSecurity(ORIGINAL_SETTINGS_SECURITY_FILE);
        }

        if (settingsSecurity != null) {
            String encodedMasterPassword = settingsSecurity.getMaster();
            String plainTextMasterPassword = decodeMasterPassword(encodedMasterPassword);

            List<Server> servers = settings.getServers();

            for (Server server : servers) {
                if (serverId.equals(server.getId())) {
                    String encodedServerPassword = server.getPassword();
                    String plainTextServerPassword = decodePassword(encodedServerPassword, plainTextMasterPassword);

                    return plainTextServerPassword;
                }
            }
        }

        return null;
    }

}
