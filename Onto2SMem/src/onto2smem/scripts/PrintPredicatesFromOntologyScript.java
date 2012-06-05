/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package onto2smem.scripts;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import onto2smem.util.OntologyUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PrintPredicatesFromOntologyScript {

    private static final int EXPECTED_ARGUMENTS = 1;
    
    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args.length != EXPECTED_ARGUMENTS) {
                System.err.println("Wrong number of arguments. Expected " + EXPECTED_ARGUMENTS + ", but found: " + args.length);
                printUsage();
                System.exit(1);
            }

            String path = args[0];

            OntModel model = OntologyUtil.getOntologyModel(path);
            
            ExtendedIterator<OntProperty> propertiesIt = model.listAllOntProperties();
            
            while (propertiesIt.hasNext()) {
                OntProperty prop = propertiesIt.next();
                
                if (prop.getLabel(null) != null) {
                    System.out.println(prop.getLocalName() +" [label: "+prop.getLabel(null)+"]");
                } else {
                    System.out.println(prop.getLocalName() +" [no label]");
                }
                
                
            }
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
        System.err.println("    java -Xmx2000m -jar PrintPredicatesFromOntologyScript.jar </path/to/owl/file>");
        System.err.println();
        System.err.println("DESCRIPTION:");
        System.err.println("    Print all predicates from ontology.");
        System.err.println();
        System.err.println("EXIT CODES:");
        System.err.println("    0: Exited normally");
        System.err.println("    1: Wrong number of arguments");
        System.err.println("    2: Unknown error (see stderr for details)");
        System.err.println();
    }
}
