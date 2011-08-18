package mc.now.util;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class CollisionCheck {

  /**
   * This can be used to check for file collisions between optional/required mods.
   */
  public static void main( String[] args ) {
    String dirstr = "./mods/extra";
    
    File dir = new File(dirstr);
    
    Map<String,String> parentMap = new HashMap<String, String>();
    for (File moddir : dir.listFiles()) {
      if (!moddir.isDirectory()) {
        continue;
      }
      String parent = moddir.getName();
      System.out.println(parent);
      
      Queue<File> queue = new LinkedList<File>();
      for (File f : moddir.listFiles()) {
        queue.add( f );
      }
      
      while (!queue.isEmpty()) {
        File f = queue.poll();
        if (f.getName().startsWith( "." )) {
          continue;
        }
        if (f.isDirectory()) {
          for (File g : f.listFiles()) {
            queue.add( g );
          }
        } else {
          String child = f.getPath().substring( moddir.getPath().length() );
          if (!parentMap.containsKey( child )) {
            parentMap.put(child,parent);
          } else {
            String other = parentMap.get(child);
            System.err.printf("\tTried to add %s to %s but it already belonged to %s\n",child, parent, other);
          }
        }
      }
    }
  }

}
