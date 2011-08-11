package mc.now.util;

import org.apache.commons.io.FilenameUtils;

public class PlatformUtil {

  public static enum OS {
    Mac,Linux,Windows;
  }
  
  public static OS currentOS;
  
  private static String mcFolder = null;
  
  static {
    String osname = System.getProperty( "os.name" ).toLowerCase();
    if (osname.startsWith( "mac" )) {
      currentOS = OS.Mac;
    } else if (osname.startsWith( "linux" )) {
      currentOS = OS.Linux;
    } else if (osname.startsWith( "win" )) {
      currentOS = OS.Windows;
    } else {
      throw new RuntimeException( "Unknown OS: " + osname );
    }
    switch(currentOS) {
      case Mac: mcFolder = System.getProperty( "user.home" )+"/Library/Application Support/minecraft/"; break;
      case Linux: mcFolder = System.getProperty( "user.home" )+"/.minecraft/"; break;
      case Windows: mcFolder = System.getenv("APPDATA") + "\\.minecraft\\"; break;
    }
  }
  
  public static String getMinecraftFolder() {
    return mcFolder;
  }

  public static String getMinecraftJar() {
    return FilenameUtils.normalize( FilenameUtils.concat(getMinecraftFolder(),"bin/minecraft.jar")) ;
  }
  
  public static String getMinecraftModsFolder() {
    return FilenameUtils.normalize( FilenameUtils.concat(getMinecraftFolder(),"mods")) ;
  }
  
  public static void setMinecraftFolder(String target) {
    mcFolder = target;
  }
  
}
