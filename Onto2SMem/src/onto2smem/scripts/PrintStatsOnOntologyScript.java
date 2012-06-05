/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package onto2smem.scripts;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import onto2smem.util.OntologyUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PrintStatsOnOntologyScript {

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
            
            File inputFile = new File(path);

            OntModel model = OntologyUtil.getOntologyModel(path);
            ExtendedIterator<Ontology> ontologiesIt = model.listOntologies();

            while (ontologiesIt.hasNext()) {
                Ontology ontology = ontologiesIt.next();
                println("Ontology <" + ontology.getURI() + ">");
                println("   * "+inputFile.getAbsolutePath()+": "+inputFile.length()+" bytes");
                println();
            }

            handleResources(model);
            handleProperties(model);

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
     * @param model
     */
    private static void handleProperties(final OntModel model) {
        ExtendedIterator<OntProperty> propertiesIt = model.listAllOntProperties();

        int propertyCount = 0;

        // Get count
        while (propertiesIt.hasNext()) {
            propertiesIt.next();
            propertyCount++;
        }

        // -------------------------------------------------------------------------------
        //  Print: properties
        // -------------------------------------------------------------------------------
        {
            println("Properties (" + propertyCount + ")");

            propertiesIt = model.listAllOntProperties();

            Set<OntProperty> datatypePropertiesSet = new HashSet(),
                    functionalPropertiesSet = new HashSet(),
                    inverseFunctionalPropertiesSet = new HashSet(),
                    objectPropertiesSet = new HashSet(),
                    symmetricPropertiesSet = new HashSet(),
                    transitivePropertiesSet = new HashSet();

            // Get examples
            while (propertiesIt.hasNext()) {
                OntProperty property = propertiesIt.next();

                if (property.isDatatypeProperty()) {
                    datatypePropertiesSet.add(property);
                }
                if (property.isFunctionalProperty()) {
                    functionalPropertiesSet.add(property);
                }
                if (property.isInverseFunctionalProperty()) {
                    inverseFunctionalPropertiesSet.add(property);
                }
                if (property.isObjectProperty()) {
                    objectPropertiesSet.add(property);
                }
                if (property.isSymmetricProperty()) {
                    symmetricPropertiesSet.add(property);
                }
                if (property.isTransitiveProperty()) {
                    transitivePropertiesSet.add(property);
                }
            }

            println("   * datatype: " + datatypePropertiesSet.size());
            printPropertyExamples(datatypePropertiesSet);
            println("   * functional: " + functionalPropertiesSet.size());
            printPropertyExamples(functionalPropertiesSet);
            println("   * inverse functional: " + inverseFunctionalPropertiesSet.size());
            printPropertyExamples(inverseFunctionalPropertiesSet);
            println("   * object: " + objectPropertiesSet.size());
            printPropertyExamples(objectPropertiesSet);
            println("   * symmetric: " + symmetricPropertiesSet.size());
            printPropertyExamples(symmetricPropertiesSet);
            println("   * transitive: " + transitivePropertiesSet.size());
            printPropertyExamples(transitivePropertiesSet);
            println();
        }
    }

    /**
     * 
     * @param model
     */
    private static void handleResources(final OntModel model) {

        // Have to collect in set so get all unique resources
        // from subjects and objects iterators.
        Set<Resource> allResources = new HashSet();

        Set<RDFNode> nonResourceNodes = new HashSet();

        ResIterator subjectIt = model.listSubjects();

        while (subjectIt.hasNext()) {
            Resource resource = subjectIt.nextResource();
            allResources.add(resource);
        }

        NodeIterator objectIt = model.listObjects();

        while (objectIt.hasNext()) {
            RDFNode node = objectIt.nextNode();

            // Exclude literals
            if (node.isResource()) {
                Resource resource = node.as(Resource.class);
                allResources.add(resource);
            } else {
                nonResourceNodes.add(node);
            }
        }

        // -------------------------------------------------------------------------------
        //  Print: resources
        // -------------------------------------------------------------------------------
        {
            println("Resources (" + allResources.size() + ")");

            int anonymousResourceCount = 0;
            Set<Resource> namedResources = new HashSet();

            for (Resource resource : allResources) {
                if (resource.isAnon()) {
                    anonymousResourceCount++;
                } else {
                    namedResources.add(resource);
                }
            }
            println("   * named: " + namedResources.size());
            printResourceExamples(namedResources);
            println("   * anonymous: " + anonymousResourceCount);
            println();
        }

        // -------------------------------------------------------------------------------
        //  Print: literals
        // -------------------------------------------------------------------------------
        {
            println("Literals (" + nonResourceNodes.size() + ")");
            println();
        }
    }

    /**
     * 
     * @param line
     */
    private static void println(String line) {
        if (line != null) {
            System.out.println(line);
        } else {
            System.out.println();
        }
    }

    /**
     * 
     */
    private static void println() {
        println(null);
    }

    /**
     * 
     * @param resourcesSet
     */
    private static void printPropertyExamples(Set<OntProperty> propertiesSet) {
        List<OntProperty> propertiesList = asPropertiesShuffledList(propertiesSet);

        int maxExamples = 3;

        if (propertiesList.size() > 0) {
            StringBuffer example = new StringBuffer();

            OntProperty property = propertiesList.get(0);
            example.append(property.getLocalName());

            for (int i = 1; i < propertiesList.size() && i < maxExamples; i++) {
                property = propertiesList.get(i);
                example.append(", " + property.getLocalName());
            }
            System.out.println("        - Example" + (propertiesList.size() == 1 ? "" : "s") + ": " + example.toString());
        }
    }

    /**
     * 
     * @param resourcesSet
     */
    private static void printResourceExamples(Set<Resource> resourcesSet) {
        List<Resource> propertiesList = asResourcesShuffledList(resourcesSet);

        int maxExamples = 3;

        if (propertiesList.size() > 0) {
            StringBuffer example = new StringBuffer();

            Resource resource = propertiesList.get(0);
            example.append(resource.getLocalName());

            for (int i = 1; i < propertiesList.size() && i < maxExamples; i++) {
                resource = propertiesList.get(i);
                example.append(", " + resource.getLocalName());
            }
            System.out.println("        - Example" + (propertiesList.size() == 1 ? "" : "s") + ": " + example.toString());
        }
    }

    /**
     * 
     * @param set
     * @return
     */
    private static List<OntProperty> asPropertiesShuffledList(Set<OntProperty> set) {
        List<OntProperty> list = new ArrayList(set.size());
        list.addAll(set);
        Collections.shuffle(list);
        return list;
    }

    /**
     * 
     * @param set
     * @return
     */
    private static List<Resource> asResourcesShuffledList(Set<Resource> set) {
        List<Resource> list = new ArrayList(set.size());
        list.addAll(set);
        Collections.shuffle(list);
        return list;
    }

    /**
     * 
     */
    public static void printUsage() {
        System.err.println();
        System.err.println("USAGE:");
        System.err.println("    java -Xmx2000m -jar PrintStatsOnOntologyScript.jar </path/to/owl/file>");
        System.err.println();
        System.err.println("DESCRIPTION:");
        System.err.println("    Print useful summary information about an ontology, including counts for properties and resources as well as some examples.");
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
