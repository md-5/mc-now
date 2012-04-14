package mc.now.util;

import java.io.File;
import java.util.*;

public class CollisionCheck {

    /**
     * This can be used to check for file collisions between optional/required
     * mods.
     */
    public static void main(String[] args) {
        String dirstr = "./mods/extra";

        File dir = new File(dirstr);

        Map<String, String> parentMap = new HashMap<String, String>();
        for (File moddir : dir.listFiles()) {
            if (!moddir.isDirectory()) {
                continue;
            }
            String parent = moddir.getName();
            System.out.println(parent);

            Queue<File> queue = new LinkedList<File>();
            queue.addAll(Arrays.asList(moddir.listFiles()));

            while (!queue.isEmpty()) {
                File f = queue.poll();
                if (f.getName().startsWith(".")) {
                    continue;
                }
                if (f.isDirectory()) {
                    queue.addAll(Arrays.asList(f.listFiles()));
                } else {
                    String child = f.getPath().substring(moddir.getPath().length());
                    if (!parentMap.containsKey(child)) {
                        parentMap.put(child, parent);
                    } else {
                        String other = parentMap.get(child);
                        System.err.printf("\tTried to add %s to %s but it already belonged to %s\n", child, parent, other);
                    }
                }
            }
        }
    }
}
