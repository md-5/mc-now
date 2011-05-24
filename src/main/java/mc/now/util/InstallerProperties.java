package mc.now.util;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class InstallerProperties {

  private static final Logger LOGGER = Logger.getLogger( InstallerProperties.class );
  
  private static final Properties properties;
  
  static {
    properties = new Properties();
    try {
      properties.load( new FileInputStream( "installer.properties" ) );
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
    return properties.getProperty( "installer.logo","logo.png" );
  }
  
  public static String getInitTextFile() {
    return properties.getProperty( "installer.init_text","init_text.txt" );
  }
  
  public static String getFrameTitle() {
    return properties.getProperty( "installer.frame_title", "Minecraft Modpack Installer" );
  }
  
}
