package mc.now.util;

import org.apache.commons.io.FilenameUtils;

public class PlatformUtil {

  public static enum OS {
    Mac,Linux,Windows;
  }
  
  public static OS currentOS;
  
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
  }
  
  public static String getMinecraftFolder() {
    switch(currentOS) {
      case Mac: return System.getProperty( "user.home" )+"/Library/Application Support/minecraft/";
      case Linux: return System.getProperty( "user.home" )+"/.minecraft/";
      case Windows: return System.getenv("APPDATA") + "\\.minecraft\\";
      default: return null;
    }
  }

  public static String getMinecraftJar() {
    return FilenameUtils.normalize( FilenameUtils.concat(getMinecraftFolder(),"bin/minecraft.jar")) ;
  }
  
  public static String getMinecraftModsFolder() {
    return FilenameUtils.normalize( FilenameUtils.concat(getMinecraftFolder(),"mods")) ;
  }
  
}
