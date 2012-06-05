/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package onto2smem.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import onto2smem.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PrintCommonEdgesFromDeclarativeAddFilesScript {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                System.exit(1);
            }

            Set<String> pathsToDeclaraativeAddFiles = new HashSet();

            for (String p : args) {
                File f = new File(p);

                if (!f.exists()) {
                    System.err.println("File does not exist: " + p);
                    printUsage();
                    System.exit(2);
                }

                pathsToDeclaraativeAddFiles.add(p);
            }

            // Keep track of all the 
            Map<String, CountsForFile> fileToCountsMap = new HashMap();

            // Step 1: read in all edges with counts from all files
            for (String path : pathsToDeclaraativeAddFiles) {
                final File file = new File(path);
                final long start = System.currentTimeMillis();
                System.out.println("Processing file: " + file.getAbsolutePath());

                CountsForFile counts = new CountsForFile();
                fileToCountsMap.put(path, counts);

                BufferedReader reader = null;

                try {
                    reader = new BufferedReader(new FileReader(file));

                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        String edge = parseEdgeFromLine(line);

                        if (edge != null) {
                            Integer count = counts.get(edge);

                            if (count == null) {
                                counts.put(edge, 1);
                            } else {
                                counts.put(edge, count + 1);
                            }
                        }
                    }
                } finally {
                    IOUtil.safeClose(reader);
                }

                double secondsEllapsed = ((double) System.currentTimeMillis() - start) / 1000.0;

                System.out.println("    Finished " + file.getName() + ", took: " + secondsEllapsed + " seconds.");
            }
            
            // Step 2: build set of all common edges
            Set<String> allCommonEdges = new HashSet();
            
            // Just pick any. Since we are interested in all common edges, we only need to compare edges from one with all others.
            String path = fileToCountsMap.keySet().toArray(new String[0])[0];
            CountsForFile counts = fileToCountsMap.get(path);
            
            allCommonEdges.addAll(counts.keySet());
            
            EDGES:
            for (String edge : counts.keySet()) {
                FILES:
                for (String otherPath : fileToCountsMap.keySet()) {
                    if (otherPath.equals(path)) {
                        continue FILES;
                    }
                    CountsForFile otherCounts = fileToCountsMap.get(otherPath);
                    
                    if (!otherCounts.containsKey(edge)) {
                        allCommonEdges.remove(edge);
                        continue EDGES;
                    }
                }
            }
            
            // Step 3: print details
            System.out.println();
            System.out.println("Total common edges: "+allCommonEdges.size());
            for (String edge : allCommonEdges) {
                System.out.println();
                System.out.println(edge);
                
                int num = 0;
                for (String nextPath : fileToCountsMap.keySet()) {
                    CountsForFile nextCounts = fileToCountsMap.get(nextPath);
                    int count = nextCounts.get(edge);
                    System.out.println("    ["+(++num)+"]"+nextPath+" -> "+count);
                }
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(3);
        }

        System.exit(0);
    }

    /**
     * <p>Looks for edge, ^someword, and returns it. Else, returns null.</p>
     * @param line
     * @return
     */
    private static String parseEdgeFromLine(String line) {
        if (line == null) {
            return null;
        }

        line = line.trim();

        int start = line.indexOf('^');

        if (start != -1) {

            // Find space
            int last = start;

            FIND_END_WORD:
            for (; last + 1 < line.length(); last++) {
                if (line.charAt(last + 1) == ' ') {
                    break FIND_END_WORD;
                }
            }

            return line.substring(start, last + 1);
        }

        return null;
    }

    /**
     * <p>Simple typdef.</p>
     * <ul>
     *   <li>The edge</li>
     *   <li>The number of instances</li>
     * </ul>
     */
    static class CountsForFile extends HashMap<String, Integer> {
    }

    /**
     * 
     */
    public static void printUsage() {
        System.err.println();
        System.err.println("USAGE:");
        System.err.println("    java -Xmx512m -jar PrintCommonEdgesFromDeclarativeAddFilesScript.jar </path/to/declarative-add/file> [</path/to/declarative-add/file> ...]");
        System.err.println();
        System.err.println("DESCRIPTION:");
        System.err.println("    Prints all the common edges in one or more than one declartive add file. If only one file, prints all edges. Provides counts for each file.");
        System.err.println();
        System.err.println("    All normal output printed to standard output; all usage information and errors printed to standard error.");
        System.err.println();
        System.err.println("EXIT CODES:");
        System.err.println("    0: Exitted normally");
        System.err.println("    1: Missing required arguments");
        System.err.println("    2: Problem with argument");
        System.err.println("    3: Unknown error (see stderr for details)");
        System.err.println();
    }
}
