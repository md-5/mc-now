package mc.now.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.swing.JTextArea;


import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class InstallScript {
  
  private static final Logger LOGGER = Logger.getLogger( InstallScript.class );
  public static void backupMCJar( String mcjar ) throws NoSuchAlgorithmException, IOException {
    FileUtils.copyFile( new File(mcjar), new File(mcjar+"_BACKUP") );
  }

  public static void repackMCJar( String tmpdir, String mcjar ) throws IOException {
    byte[] dat = new byte[4*1024];
    FileUtils.copyFile( new File(mcjar), new File(mcjar+"_BACKUP") );
    
    JarOutputStream jarout = new JarOutputStream( FileUtils.openOutputStream( new File(mcjar) ) );
    
    Queue<File> queue = new LinkedList<File>();
    for (File f : new File(tmpdir).listFiles()) {
      queue.add( f );
    }
    
    while (!queue.isEmpty()) {
      File f = queue.poll();
      
      String name = f.getPath().substring( tmpdir.length() );
      name = name.replace( "\\", "/");
      if (f.isDirectory() && !name.endsWith( "/" )) {
        name = name + "/";
      }
      JarEntry entry = new JarEntry( name );
      jarout.putNextEntry( entry );
      
      if (f.isDirectory()) {
        for (File child : f.listFiles()) {
          queue.add( child );
        }
      } else {
        FileInputStream in = new FileInputStream( f );
        int len = -1;
        while ((len=in.read( dat )) > 0) {
          jarout.write( dat, 0, len );
        }
        in.close();
      }
      jarout.closeEntry();
    }
    jarout.close();
    
  }

  public static void unpackMCJar(String tmpdir, String mcjar) throws IOException {
    byte[] dat = new byte[4*1024];
    JarFile jar = new JarFile( mcjar );
    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.startsWith( "META-INF" )) {
        continue;
      }
      InputStream in = jar.getInputStream( entry );
      File dest = new File( tmpdir + name );
      if (entry.isDirectory()) {
        dest.mkdirs();
      } else if (!dest.getParentFile().exists()) {
        dest.getParentFile().mkdirs();
      }
      FileOutputStream out = new FileOutputStream(dest);
      int len = -1;
      while ((len = in.read(dat)) > 0) {
        out.write(dat,0,len);
      }
      out.flush();
      out.close();
      in.close();
    }
  }
  
  public static void copyCoreModClasses(String coreloc, String tmpdir) throws IOException {
    FileUtils.copyDirectory( new File(coreloc), new File(tmpdir) );
  }
  
  public static void copyCoreModeFiles(String corefiles, String tmpdir) throws IOException {
    FileUtils.copyDirectory( new File(corefiles), new File(tmpdir) );
  }

  public static void guiInstall( List<String> mods, JTextArea text ) {
    
    //TODO backup minecraft folder.
    
    Random rand = new Random();
    File tmp = new File("./"+Integer.toHexString( rand.nextInt(Integer.MAX_VALUE) ));
    while (tmp.exists()) {
      tmp = new File("./"+Integer.toHexString( rand.nextInt(Integer.MAX_VALUE) ));
    }

    if (!tmp.mkdirs()) {
      text.append( "Error creating temp directory!" );
      return;
    }
    
    String tmpdir = tmp.getPath() + "/";
    File mcDir = new File(PlatformUtil.getMinecraftFolder());
    String mcjar = PlatformUtil.getMinecraftJar();
    
    try {
      text.append( "Unpacking minecraft.jar\n" );
      unpackMCJar( tmpdir, mcjar);
    } catch ( IOException e ) {
      text.append("Error unpacking minecraft.jar!");
      LOGGER.error("Install Error",e);
      return;
    }
    
    File reqDir = new File("./mods/required/");
    
    //TODO specific ordering required!
    for (File mod : reqDir.listFiles()) {
      if (!mod.isDirectory()) {
        continue;
      }
      String name = mod.getName();
      text.append("...Installing " + name+"\n");
      try {
        installMod( mod, tmp, mcDir );
      } catch ( IOException e ) {
        text.append("Error installing "+name+"!\n");
        LOGGER.error("Install Error",e);
        return;
      }
    }
    
    if (!mods.isEmpty()) {
      text.append("Installing extra mods\n");
    }
    //TODO specific ordering required!
    for (String name : mods) {
      File mod = new File("./mods/extra/"+name);
      text.append("...Installing " + name+"\n");
      try {
        installMod( mod, tmp, mcDir );
      } catch ( IOException e ) {
        text.append("Error installing "+name+"!\n");
        LOGGER.error("Install Error",e);
        return;
      }
    }
    

    text.append("Repacking minecraft.jar\n");
    try {
      repackMCJar(tmpdir,mcjar);
    } catch ( IOException e ) {
      text.append("Error repacking minecraft.jar!");
      LOGGER.error("Install Error",e);
      return;
    }

    text.append("Deleting temporary files\n");
    
    try {
      FileUtils.deleteDirectory( tmp );
    } catch ( IOException e ) {
      text.append("Error deleting temporary files!\n");
      LOGGER.error("Install Error",e);
      return;
    }
    text.append( "Finished!" );
    
  }
  
  private static void installMod(File modDir, File jarDir, File mcDir) throws IOException {
    for (File dir : modDir.listFiles()) {
      if (!dir.isDirectory()) {
        continue;
      }
      if (dir.getName().equals( "jar" )) {
        FileUtils.copyDirectory( dir, jarDir );
      } else if (dir.getName().equals( "resources" )) {
        FileUtils.copyDirectory( dir, mcDir );
      }
    }
  }
  
}
