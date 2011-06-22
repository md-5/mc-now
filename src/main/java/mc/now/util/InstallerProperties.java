package mc.now.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class InstallerProperties {

  private static final Logger LOGGER = Logger.getLogger( InstallerProperties.class );
  
  private static String installerDir;
  
  private static final Properties properties;
  
  static {
    installerDir = "./";
    properties = new Properties();
    try {
      //TODO This feels extremely ugly
      URL url = InstallerProperties.class.getProtectionDomain().getCodeSource().getLocation();
      File f = new File(new URI(url.toString()));
      File dir = f.getParentFile();
      installerDir = dir.getPath();
      properties.load( new FileInputStream(FilenameUtils.concat( installerDir, "config/installer.properties" )) );
    } catch (Exception e) {
      LOGGER.error( "Error loading installer.properties file",e );
    }
  }

  public static String getMinecraftVersion() {
    return properties.getProperty( "mc.version" );
  }
  
  public static String getMinecraftJarMD5() {
    return properties.getProperty( "mc.jar.md5" );
  }

  public static String getLogoFile() {
    return FilenameUtils.concat( installerDir, "config/logo.png" );
  }
  
  public static String getInitTextFile() {
    return FilenameUtils.concat( installerDir, "config/init_text.txt" );
  }
  
  public static String getFrameTitle() {
    return properties.getProperty( "installer.frame_title", "Minecraft Modpack Installer" );
  }
  
  public static String getInstallerDir() {
    return installerDir;
  }
  
}
