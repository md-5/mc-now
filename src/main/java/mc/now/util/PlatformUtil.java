package mc.now.util;

public class PlatformUtil {

  public static enum OS {
    Mac,Linux,Windows,DEV;
  }
  
  //TODO
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
      case DEV: return "./minecraft/";
      default: return null;
    }
  }
}
