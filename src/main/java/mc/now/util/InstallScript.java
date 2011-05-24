package mc.now.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.swing.JProgressBar;
import javax.swing.JTextArea;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class InstallScript {
  
  public static final String MODS_FOLDER = FilenameUtils.normalize( "./mods/" );
  public static final String REQUIRED_MODS_FOLDER = FilenameUtils.concat( MODS_FOLDER, "required/" );
  public static final String EXTRA_MODS_FOLDER = FilenameUtils.concat( MODS_FOLDER, "extra/" );
  
  private static final Logger LOGGER = Logger.getLogger( InstallScript.class );

  public static void repackMCJar( File tmp, File mcjar ) throws IOException {
    byte[] dat = new byte[4*1024];
    
    JarOutputStream jarout = new JarOutputStream( FileUtils.openOutputStream( mcjar ) );
    
    Queue<File> queue = new LinkedList<File>();
    for (File f : tmp.listFiles()) {
      queue.add( f );
    }
    
    while (!queue.isEmpty()) {
      File f = queue.poll();
      if (f.isDirectory()) {
        for (File child : f.listFiles()) {
          queue.add( child );
        }
      } else {
        //TODO need a better way to do this
        String name = f.getPath().substring( tmp.getPath().length()+1 );
        //TODO is this formatting really required for jars?
        name = name.replace( "\\", "/");
        if (f.isDirectory() && !name.endsWith( "/" )) {
          name = name + "/";
        }
        JarEntry entry = new JarEntry( name );
        jarout.putNextEntry( entry );
        
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

  public static void unpackMCJar(File tmpdir, File mcjar) throws IOException {
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
      File dest = new File( FilenameUtils.concat( tmpdir.getPath(), name ) );
      if (entry.isDirectory()) {
        //I don't think this actually happens
        LOGGER.warn( "Found a directory while iterating over jar." );
        dest.mkdirs();
      } else if (!dest.getParentFile().exists()) {
        if (!dest.getParentFile().mkdirs()) {
          throw new IOException( "Couldn't create directory for " + name );
        }
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

  private static File getTempDir() throws IOException {
    Random rand = new Random();
    String hex = Integer.toHexString( rand.nextInt(Integer.MAX_VALUE) );
    File tmp = new File(FilenameUtils.normalize( "./"+hex+"/"));
    int t = 0;
    while (tmp.exists() && t < 10) {
      hex = Integer.toHexString( rand.nextInt(Integer.MAX_VALUE) );
      tmp = new File(FilenameUtils.normalize( "./"+hex+"/"));
      t++;
    }
    if (tmp.exists()) {
      throw new IOException( "Error creating temporary folder. Too many failures." );
    }
    return tmp;
  }
  
  public static String preInstallCheck() {
    //TODO check the number in .minecraft/bin/version file?
    try {
      InputStream mcJarIn = new FileInputStream( PlatformUtil.getMinecraftJar() );
      String digest = DigestUtils.md5Hex( mcJarIn );
      mcJarIn.close();
      boolean jarValid = InstallerProperties.getMinecraftJarMD5().equalsIgnoreCase( digest );
      File modsDir = new File(PlatformUtil.getMinecraftModsFolder());
      boolean noMods = !modsDir.exists() || modsDir.listFiles().length == 0;
      String msg = null;
      if (!jarValid) {
        msg = String.format("Your minecraft.jar has been modified or is not the correct version (%s).",InstallerProperties.getMinecraftVersion());
      }
      if (!noMods) {
        msg = (msg == null ? "" : msg+"\n") + "You already have mods installed to the minecraft mods folder.";
      }
      return msg;
     } catch ( Exception e ) {
      LOGGER.error( "Pre-installation check error",e );
      return "There was an error verifying your minecraft installation. " + e.getMessage();
    }
  }
  
  //TODO In general this seems to be slower than it should be
  public static void guiInstall( List<String> mods, JTextArea text, JProgressBar progressBar ) {
    
    try {
      createBackup();
    } catch ( IOException e ) {
      text.append( "Failed to create backup copies of minecraft.jar and mods folder");
      LOGGER.error( "Failed to create backup copies of minecraft.jar and mods folder", e );
      return;
    }
    
    File tmp;
    try {
      tmp = getTempDir();
    } catch ( IOException e ) {
      text.append( "Error creating temp directory!" );
      LOGGER.error("Install Error",e);
      return;
    }
    if (!tmp.mkdirs()) {
      text.append( "Error creating temp directory!" );
      return;
    }
    
    File mcDir = new File(PlatformUtil.getMinecraftFolder());
    File mcJar = new File(PlatformUtil.getMinecraftJar());
    File reqDir = new File(REQUIRED_MODS_FOLDER);
    
    int reqMods = 0;
    for (File f : reqDir.listFiles()) {
      if (f.isDirectory()) {
        reqMods++;
      }
    }
    //3 "other" steps: unpack, repack, delete temp
    int baseTasks = 3;
    int taskSize =  reqMods + mods.size() + baseTasks;
    
    progressBar.setMinimum( 0 );
    progressBar.setMaximum( taskSize );
    int task = 1;
    
    try {
      text.append( "Unpacking minecraft.jar\n" );
      unpackMCJar( tmp, mcJar);
      progressBar.setValue( task++ );
  
      text.append("Installing Core mods\n");
      //TODO specific ordering required!
      for (File mod : reqDir.listFiles()) {
        if (!mod.isDirectory()) {
          continue;
        }
        String name = mod.getName();
        text.append("...Installing " + name+"\n");
        installMod( mod, tmp, mcDir );
        progressBar.setValue( task++ );
      }
      
      if (!mods.isEmpty()) {
        text.append("Installing Extra mods\n");
        //TODO specific ordering required!
        for (String name : mods) {
          File mod = new File(FilenameUtils.normalize( FilenameUtils.concat( EXTRA_MODS_FOLDER, name ) ));
          text.append("...Installing " + name+"\n");
          installMod( mod, tmp, mcDir );
          progressBar.setValue( task++ );
        }
      }
  
      text.append("Repacking minecraft.jar\n");
      repackMCJar(tmp,mcJar);
      progressBar.setValue( task++ );
    } catch (Exception e) {
      text.append( "!!!Error installing mods!!!");
      LOGGER.error( "Installation error",e );
      try {
        restoreBackup();
      } catch ( IOException ioe ) {
        text.append( "Error while restoring backup files minecraft.jar.backup and mods_backup folder\n!" );
        LOGGER.error( "Error while restoring backup files minecraft.jar.backup and mods_backup folder!", ioe );
      }
    }

    text.append("Deleting temporary files\n");
    try {
      FileUtils.deleteDirectory( tmp );
      progressBar.setValue( task++ );
    } catch ( IOException e ) {
      text.append("Error deleting temporary files!\n");
      LOGGER.error("Install Error",e);
      return;
    }
    text.append( "Finished!" );
    
  }
  
  //TODO move this elsewhere and make it more general
  private static final String[] otherThingsToBackup = {"millenaire"};

  private static void createBackup() throws IOException {
    //TODO what other folders to backup?
    FileUtils.copyFile( new File(PlatformUtil.getMinecraftJar()), new File(PlatformUtil.getMinecraftJar()+".backup") );
    File mods = new File(PlatformUtil.getMinecraftModsFolder());
    File modsBackup = new File(PlatformUtil.getMinecraftModsFolder() + "_backup/");
    if (modsBackup.exists()) {
      FileUtils.deleteDirectory( modsBackup );
    }
    if (mods.exists()) {
      FileUtils.copyDirectory(mods,modsBackup);
    }
    
    for (String name : otherThingsToBackup) {
      String fname = FilenameUtils.normalize( FilenameUtils.concat( PlatformUtil.getMinecraftFolder(), name ) );
      String fnameBackup = fname + "_backup";
      File f = new File(fname);
      File backup = new File( fnameBackup );
      if (backup.exists()) {
        FileUtils.deleteDirectory( backup );
      }
      if (f.exists()) {
        FileUtils.copyDirectory( f, backup );
      }
    }
  }
  
  private static void restoreBackup() throws IOException {
    //TODO what other folders to restore?
    FileUtils.copyFile( new File(PlatformUtil.getMinecraftJar()+".backup"),new File(PlatformUtil.getMinecraftJar()) );
    File mods = new File(PlatformUtil.getMinecraftModsFolder());
    File modsBackup = new File(PlatformUtil.getMinecraftModsFolder() + "_backup");
    if (modsBackup.exists()) {
      FileUtils.deleteDirectory( mods );
      FileUtils.copyDirectory(modsBackup,mods);
    }
    for (String name : otherThingsToBackup) {
      String fname = FilenameUtils.normalize( FilenameUtils.concat( PlatformUtil.getMinecraftFolder(), name ) );
      String fnameBackup = fname + "_backup";
      File f = new File(fname);
      File backup = new File( fnameBackup );
      if (backup.exists()) {
        FileUtils.copyDirectory( backup, f );
      }
    }
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
