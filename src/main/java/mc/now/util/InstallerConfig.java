package mc.now.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/**
 * Contains settings from properties, the detected OS, and the locations of
 * various important files/directories.
 */
public class InstallerConfig {

    private static final Logger LOGGER = Logger.getLogger(InstallerConfig.class);

    private static enum OS {

        Mac, Linux, Windows;
    }
    /**
     * Location of the installer, set in the static initializer block.
     */
    private static final String installerDir;
    /**
     * The installer's properties, loaded in the static initializer block.
     */
    private static final Properties properties;
    /**
     * The current OS, set in the static initializer block via
     * System.getProperty("os.name")
     */
    public static final OS currentOS;
    /**
     * The target minecraft installation. Defaults to the main minecraft folder,
     * but can be set by the user in the GUI.
     */
    private static String mcFolder = null;

    static {
        /*
         * Detect what the current OS is.
         */
        String osname = System.getProperty("os.name").toLowerCase();
        if (osname.startsWith("mac")) {
            currentOS = OS.Mac;
        } else if (osname.startsWith("linux")) {
            currentOS = OS.Linux;
        } else if (osname.startsWith("win")) {
            currentOS = OS.Windows;
        } else {
            throw new RuntimeException("Unknown OS: " + osname);
        }
        switch (currentOS) {
            case Mac:
                mcFolder = System.getProperty("user.home") + "/Library/Application Support/minecraft/";
                break;
            case Linux:
                mcFolder = System.getProperty("user.home") + "/.minecraft/";
                break;
            case Windows:
                mcFolder = System.getenv("APPDATA") + "\\.minecraft\\";
                break;
        }

        /*
         * Get the location of the installer and load the properties.
         */
        properties = new Properties();
        try {
            //TODO This feels extremely ugly
            URL url = InstallerConfig.class.getProtectionDomain().getCodeSource().getLocation();
            File f = new File(new URI(url.toString()));
            File dir = f.getParentFile();
            installerDir = dir.getPath();
            properties.load(new FileInputStream(FilenameUtils.concat(installerDir, "config/installer.properties")));
        } catch (Exception e) {
            LOGGER.error("Error loading installer.properties file", e);
            throw new RuntimeException("Can't load properties or detect install location");
        }
    }

    /**
     * The location the installer is running from.
     *
     * @return
     */
    public static String getInstallerDir() {
        return installerDir;
    }

    /**
     * The current version of minecraft, specified in
     * config/installer.properties
     *
     * @return
     */
    public static String getMinecraftVersion() {
        return properties.getProperty("mc.version");
    }

    /**
     * The md5 hash (as a hex string) of the current unmodded minecraft.jar,
     * specified in config/installer.properties
     *
     * @return
     */
    public static String getMinecraftJarMD5() {
        return properties.getProperty("mc.jar.md5");
    }

    /**
     * The desired title of the installer. Defaults to "Minecraft Modpack
     * Installer".
     *
     * @return
     */
    public static String getFrameTitle() {
        return properties.getProperty("installer.frame_title", "Minecraft Modpack Installer");
    }

    /**
     * The image to initially display, saved at config/logo.png
     *
     * @return
     */
    public static String getLogoFile() {
        return FilenameUtils.concat(installerDir, "config/logo.png");
    }

    /**
     * Text or basic HTML to initially display, saved at config/init_text.txt
     *
     * @return
     */
    public static String getInitTextFile() {
        return FilenameUtils.concat(installerDir, "config/init_text.txt");
    }

    /**
     * The target minecraft folder to install to. Platform specific, but can be
     * changed by the user.
     *
     * @return
     */
    public static String getMinecraftFolder() {
        return mcFolder;
    }

    /**
     * Sets the target minecraft folder.
     *
     * @param target
     */
    public static void setMinecraftFolder(String target) {
        mcFolder = target;
    }

    /**
     * The location of minecraft.jar.
     *
     * @return
     */
    public static String getMinecraftJar() {
        return FilenameUtils.normalize(FilenameUtils.concat(getMinecraftFolder(), "bin/minecraft.jar"));
    }

    /**
     * The "mods" folder in the target minecraft installation.
     *
     * @return
     */
    public static String getMinecraftModsFolder() {
        return FilenameUtils.normalize(FilenameUtils.concat(getMinecraftFolder(), "mods"));
    }

    /**
     * The mods folder for this installer.
     *
     * @return
     */
    public static String getInstallerModsFolder() {
        return FilenameUtils.concat(getInstallerDir(), "mods/");
    }

    /**
     * The folder of required mods for this installer.
     *
     * @return
     */
    public static String getRequiredModsFolder() {
        return FilenameUtils.concat(getInstallerModsFolder(), "required/");
    }

    /**
     * The folder of optional mods for this installer.
     *
     * @return
     */
    public static String getExtraModsFolder() {
        return FilenameUtils.concat(getInstallerModsFolder(), "extra/");
    }
}
