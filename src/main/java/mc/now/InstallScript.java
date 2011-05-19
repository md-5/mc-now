package mc.now;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;

import mc.now.util.PlatformUtil;

public class InstallScript {

  public static void main( String[] args ) throws IOException {
    
//    PlatformUtil.currentOS = OS.DEV;
    
    //TODO better tmp dir
    String tmpdir = "./tmp/";
    String mcdir = PlatformUtil.getMinecraftFolder();
    
    String coreloc = "./resources/";
    String mcjar = mcdir + "bin/minecraft.jar";
    
    unpackMCJar( tmpdir, mcjar );
    copyCoreModClasses(coreloc+"jar/", tmpdir);
    repackMCJar(tmpdir, mcjar);
    
    copyCoreModeFiles(coreloc+"maindir/",mcdir);
    
    FileUtils.deleteDirectory( new File(tmpdir) );
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
      System.out.println(name);
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
      if (!dest.getParentFile().exists()) {
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
  
  
}
