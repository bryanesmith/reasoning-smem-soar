/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package onto2smem.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import onto2smem.util.IOUtil;
import onto2smem.util.SMemUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class GetChildCountInstancesScript {

    private static final int EXPECTED_ARGUMENTS = 1;
    private static final boolean IS_INCLUDE_ANONYMOUS = true;
    private static final boolean IS_INCLUDE_WORDNET = true;
    private static final boolean IS_INCLUDE_EXTERNAL_NAME = false;

    public static void main(String[] args) {
        try {
            if (args.length != EXPECTED_ARGUMENTS) {
                System.err.println("Wrong number of arguments. Expected " + EXPECTED_ARGUMENTS + ", but found: " + args.length);
                printUsage();
                System.exit(1);
            }

            String path = args[0];
            File inputFile = new File(path);

            BufferedReader in = null;

            try {
                in = new BufferedReader(new FileReader(inputFile));

                System.out.println("\"Child count\",\"Instances\"");

                String line = null;

                boolean isReadingItem = false;
                int childCount = 0;
                Map<Integer, Integer> countInstances = new HashMap();
                String localName = null, name = null, lti = null;
                READING:
                while ((line = in.readLine()) != null) {

                    // Skip blank lines
                    if (line.trim().equals("")) {
                        continue;
                    }

                    if (line.trim().equals("smem --add {")) {

                        if (isReadingItem) {
                            throw new RuntimeException("Assertion failed: read in start of add statement, but state says currently reading item.");
                        }

                        isReadingItem = true;
                        continue READING;
                    } else if (line.trim().equals("}")) {

                        if (!isReadingItem) {
                            throw new RuntimeException("Assertion failed: read in end of add statement, but state says currently not reading item.");
                        }

                        try {
                            if (!IS_INCLUDE_ANONYMOUS && (localName == null && name == null)) {
                                continue READING;
                            }
                            
                            if (!IS_INCLUDE_WORDNET) {
                                if (localName != null && localName.startsWith("|WN30-")) {
                                    continue READING;
                                }
                                if (name != null && name.startsWith("|WN30-")) {
                                    continue READING;
                                }
                            }
                            if (!IS_INCLUDE_EXTERNAL_NAME) {
                                if (localName == null && name != null) {
                                    continue READING;
                                }
                            }
                            

                            Integer instances = countInstances.get(childCount);

                            if (instances == null) {
                                instances = Integer.valueOf(0);
                            }

                            countInstances.put(childCount, instances + 1);
                        } finally {
                            isReadingItem = false;
                            localName = null;
                            name = null;
                            lti = null;
                            childCount = 0;
                        }
                    }

                    if (!isReadingItem) {
                        continue;
                    }

                    // Assert that it has two or three tokens.
                    line = line.trim();

                    if (!line.startsWith("(") || !line.endsWith(")")) {
                        throw new RuntimeException("Assertion failed: line not surrounded by parentheses: " + line);
                    }

                    // Remove parentheses
                    line = line.substring(1, line.length() - 1).trim();

                    List<String> tokensList = new LinkedList();

                    if (line.startsWith("^")) {

                        String[] subTokens = line.split(" ");

                        if (subTokens.length < 2) {
                            throw new RuntimeException("Should be at least two or more tokens/subtokens, instead " + subTokens.length + " for line: " + line);
                        }

                        tokensList.add(subTokens[0]);

                        // Might be a |string with spaces|
                        StringBuffer val = new StringBuffer();
                        val.append(subTokens[1]);

                        for (int i = 2; i < subTokens.length; i++) {
                            val.append(" " + subTokens[i]);
                        }
                        tokensList.add(val.toString());

                    } else {
                        String[] subTokens = line.split(" ");

                        if (subTokens.length < 3) {
                            throw new RuntimeException("Should be at least three or more tokens/subtokens, instead " + subTokens.length + " for line: " + line);
                        }

                        // Save the lti perchance don't find name
                        lti = subTokens[0];

                        tokensList.add(subTokens[0]);
                        tokensList.add(subTokens[1]);

                        // Might be a |string with spaces|
                        StringBuffer val = new StringBuffer();
                        val.append(subTokens[2]);

                        for (int i = 3; i < subTokens.length; i++) {
                            val.append(" " + subTokens[i]);
                        }

                        tokensList.add(val.toString());
                    }

                    final String[] tokens = tokensList.toArray(new String[0]);
                    int tokenCount = tokens.length;

                    if (tokenCount != 2 && tokenCount != 3) {
                        throw new RuntimeException("Assertion failed, expecting two or three tokens, but found " + tokenCount + " for line: " + line);
                    }

                    // Increment child count
                    childCount++;

                    // Look for name or localname
                    String predicate = null, value = null;
                    if (tokenCount == 2) {
                        predicate = tokens[0];
                        value = tokens[1];
                    } else if (tokenCount == 3) {
                        predicate = tokens[1];
                        value = tokens[2];
                    }

                    if (!predicate.startsWith("^")) {
                        throw new RuntimeException("Assertion fails, predicate not found (" + predicate + ") for line: " + line);
                    }

                    if (predicate.equals(SMemUtil.LOCAL_NAME_PREDICATE)) {
                        localName = value;
                    } else if (predicate.equals(SMemUtil.NAME_PREDICATE)) {
                        name = value;
                    }
                }

                List<Integer> keysSorted = new ArrayList(countInstances.size());
                keysSorted.addAll(countInstances.keySet());
                Collections.sort(keysSorted);

                for (int key : keysSorted) {
                    int instances = countInstances.get(key);
                    System.out.println(key + "," + instances);
                }

            } finally {
                IOUtil.safeClose(in);
            }


            System.exit(0);

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(2);
        }
    }

    /**
     * 
     */
    public static void printUsage() {
        System.err.println();
        System.err.println("USAGE:");
        System.err.println("    java -Xmx2000m -jar GetChildCountInstancesScript.jar </path/to/declarative-add/file>");
        System.err.println();
        System.err.println("DESCRIPTION:");
        System.err.println("    Using an input declarative add file, print out a CSV with the following fields:");
        System.err.println("        * Child count");
        System.err.println("        * Instances");
        System.err.println();
        System.err.println("    All normal output printed to standard output; all usage information and errors printed to standard error.");
        System.err.println();
        System.err.println("EXIT CODES:");
        System.err.println("    0: Exitted normally");
        System.err.println("    1: Wrong number of arguments");
        System.err.println("    2: Unknown error (see stderr for details)");
        System.err.println();
    }
}
