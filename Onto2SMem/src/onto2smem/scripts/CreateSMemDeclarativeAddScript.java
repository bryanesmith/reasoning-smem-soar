/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package onto2smem.scripts;

import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import onto2smem.util.OntologyUtil;
import onto2smem.util.SMemUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class CreateSMemDeclarativeAddScript {

    private static boolean isUseNameInsteadOfLocalName = true;
    private static final int EXPECTED_ARGUMENTS = 1;
    private static boolean isPrintTracers = false;
    private static boolean isPrintedAddedStart = false;

    /**
     * 
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {

        isPrintedAddedStart = false;

        try {
            if (args.length != EXPECTED_ARGUMENTS) {
                System.err.println("Wrong number of arguments. Expected " + EXPECTED_ARGUMENTS + ", but found: " + args.length);
                printUsage();
                System.exit(1);
            }

            String path = args[0];
            OntModel model = OntologyUtil.getOntologyModel(path);

            // Build up namespaces. If there is more than one ontology, we'll always need to use these.
            Set<String> namespaces = new HashSet();
            ExtendedIterator<Ontology> ontologyIt = model.listOntologies();

            while (ontologyIt.hasNext()) {
                Ontology ontology = ontologyIt.next();
                namespaces.add(ontology.getNameSpace());
                println("# Found ontology with namespace: " + ontology.getNameSpace());
            }

            if (namespaces.size() < 1) {
                throw new RuntimeException("Assertion failed: expecting at least one ontology, instead found: " + namespaces.size());
            }

            handleResources(model, namespaces);

            println("}");

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
    private static void handleResources(final OntModel model, Set<String> namespaces) {

        // Have to collect in set so get all unique resources
        // from subjects and objects iterators.
        //
        // Maps resource to LTI
        Map<Resource, String> allResources = new HashMap();

        ResIterator subjectIt = model.listSubjects();

        int count = 1;

        // Check: subjects
        while (subjectIt.hasNext()) {
            Resource resource = subjectIt.nextResource();

            // Conditionally add, with lti, if not already contain
            if (!allResources.keySet().contains(resource)) {
                String lti = "<r" + (count++) + ">";
                allResources.put(resource, lti);
            }
        }

        NodeIterator objectIt = model.listObjects();

        // Check: objects
        while (objectIt.hasNext()) {
            RDFNode node = objectIt.nextNode();

            // Exclude literals
            if (node.isResource()) {
                Resource resource = node.as(Resource.class);

                // Conditionally add, with lti, if not already contain
                if (!allResources.keySet().contains(resource)) {
                    String lti = "<r" + (count++) + ">";
                    allResources.put(resource, lti);
                }
            }
        }

        Set<ObjectPropertyNode> objectPropertyNodes = new HashSet();

        ExtendedIterator<ObjectProperty> objPropIt = model.listObjectProperties();

        // Get all obj properties
        while (objPropIt.hasNext()) {
            ObjectProperty op = objPropIt.next();
            String opName = SMemUtil.safeQuotePredicate(op.getLocalName());
            ObjectPropertyNode node = new ObjectPropertyNode(opName);
            objectPropertyNodes.add(node);

            StmtIterator stmtIt = op.listProperties();
            while (stmtIt.hasNext()) {
                Statement stmt = stmtIt.nextStatement();

                if (stmt.getPredicate().getLocalName().equals("type")) {

                    if (!stmt.getObject().isURIResource()) {
                        throw new RuntimeException("How can a type object not be a URI resource?");
                    }

                    Resource objResource = stmt.getObject().as(Resource.class);

//                    node.addType(objResource.getURI());
                    node.addType(objResource.getLocalName());
                }
            }
        }

        // Restart counter
        int ltiCount = count;
        count = 1;

        RESOURCES:
        for (Resource thisResource : allResources.keySet()) {

            List<PredicateObject> statementsToPrint = new LinkedList();
            final String thisLti = allResources.get(thisResource);

            println();

            final String desc = SMemUtil.safeQuoteString("Adding item #" + (count++) + " of " + allResources.size() + ": " + thisLti + " " + (thisResource.isAnon() ? "anonymous" : thisResource.getLocalName()));
            if (isPrintTracers) {
                println("echo " + desc);
            }

            if (!isPrintedAddedStart) {
                println("smem --add {");
                isPrintedAddedStart = true;
            }

            println("     # " + desc);
            if (thisResource.getLocalName() != null) {

                // ----------------------------------------------------------------------------------------------------------
                // Guarenteed to be at least one. 
                //
                // PROBLEM: This is rough -- just because starts with same path doesn't mean same namespace, since
                //          can be multiple ontologies per domain. Hasn't happened yet, and the consequences are
                //          low for test agents. Later, better to figure better way to determine.
                //
                // PROBLEM: This will falsely be true WordNet resources in SUMO since namespace is local file path.
                //
                // ----------------------------------------------------------------------------------------------------------
                boolean isSameNamespace = thisResource.getNameSpace().startsWith(namespaces.toArray(new String[0])[0]);

                // Is it from this resource? If not, if there is more than one resource, we'll need to add namespace
//                if (namespaces.size() == 1 && isSameNamespace && !isUseNameInsteadOfLocalName) {
//                    String val = SMemUtil.safeQuoteLiteral(thisResource.getLocalName());
//                    System.out.println("DEBUG> 1. "+val);
//                    statementsToPrint.add(new PredicateObject(SMemUtil.LOCAL_NAME_PREDICATE, val));
//                } else {
//                    String val = thisResource.getNameSpace() + thisResource.getLocalName();
//                    System.out.println("DEBUG> 2. "+val);
//                    statementsToPrint.add(new PredicateObject(SMemUtil.NAME_PREDICATE, val));
//                }

                String val = SMemUtil.safeQuoteLiteral(thisResource.getLocalName());
                statementsToPrint.add(new PredicateObject(SMemUtil.NAME_PREDICATE, val));
            }

            StmtIterator stmtIt = model.listStatements(thisResource, (Property) null, (RDFNode) null);

            STATEMENTS:
            while (stmtIt.hasNext()) {
                Statement statement = stmtIt.nextStatement();

                if (!statement.getSubject().equals(thisResource)) {
                    throw new RuntimeException("Assertion failed: unexpected resource");
                }

                String predicateStr = SMemUtil.safeQuotePredicate(statement.getPredicate().getLocalName());

                RDFNode objectNode = statement.getObject();
                String objectLti = null;

                if (objectNode.isResource()) {
                    Resource objectResource = objectNode.as(Resource.class);

                    if (allResources.containsKey(objectResource)) {
                        objectLti = allResources.get(objectResource);
                        statementsToPrint.add(new PredicateObject("^" + predicateStr, objectLti));
                    }
                }

                if (objectLti == null) {
                    if (objectNode.isLiteral()) {

                        String objectStr = objectNode.toString();
                        if (predicateStr.equals("name")) {
                            objectStr = SMemUtil.parseNameFromURI(objectStr);
                        }

                        statementsToPrint.add(new PredicateObject("^" + predicateStr, SMemUtil.safeQuoteLiteral(objectStr)));
                    } else {
                        throw new RuntimeException("Assertion failed: object neither recognized resource nor literal.");
                    }
                }

                // Try to find the object property node, if one. (If this isn't an object property, it will be null.)
                {
                    ObjectPropertyNode opn = null;
                    for (ObjectPropertyNode candidateOpn : objectPropertyNodes) {
                        if (candidateOpn.name.equals(predicateStr)) {
                            opn = candidateOpn;
                            break;
                        }
                    }

                    if (opn != null) {
                        if (objectLti == null) {
                            throw new RuntimeException("How can object property not have a LTI for the object?");
                        }

                        SubjectObjectPair pair = opn.addNode(thisLti, objectLti);
                        if (pair == null) {
                            throw new RuntimeException("Why have problems adding object property nodes");
                        }
//                        System.out.println("DEBUG> Added "+thisLti+" ^"+opn.name+" "+objectLti);
                    }
                }
            }

            for (int i = 0; i < statementsToPrint.size(); i++) {
                PredicateObject po = statementsToPrint.get(i);
                if (i == 0) {
                    print("     (" + thisLti + " ");
                } else {
                    StringBuffer spaces = new StringBuffer();
                    for (int j = 0; j < thisLti.length() + 7; j++) {
                        spaces.append(" ");
                    }
                    print(spaces.toString());
                }

                print(po.predicate + " " + po.object);
                // If last element, print ending paren
                if (i == statementsToPrint.size() - 1) {
                    print(")");
                }
                println();
            }


        }

        println();

        // Add all the object properties
        for (ObjectPropertyNode opn : objectPropertyNodes) {
            String lti = "<r" + (ltiCount++) + ">";

            StringBuffer spaces = new StringBuffer();
            for (int j = 0; j < lti.length() + 7; j++) {
                spaces.append(" ");
            }

            println("     (" + lti + " ^propertyName " + SMemUtil.safeQuoteString(opn.name));

            for (String type : opn.types) {
                println(spaces.toString() + "^type " + SMemUtil.safeQuoteString(type));
            }

            for (SubjectObjectPair pair : opn.nodes) {
                lti = "<r" + (ltiCount++) + ">";

                println(spaces.toString() + "^next " + lti + ")");
                println();

                spaces = new StringBuffer();
                for (int j = 0; j < lti.length() + 7; j++) {
                    spaces.append(" ");
                }

                println("     (" + lti + " ^subject " + pair.subjectLTI);
                println(spaces.toString() + "^object " + pair.objectLTI);
                println(spaces.toString() + "^propertyName " + SMemUtil.safeQuoteString(opn.name));
            }
            println(spaces.toString() + "^next nil)");
            println();
        }

    } // handleResources

    private static void print(String str) {
        System.out.print(str);
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
     */
    private static class PredicateObject {

        public final String predicate, object;

        PredicateObject(String predicate, String object) {
            this.predicate = predicate;
            this.object = object;
        }
    }

    /**
     * 
     */
    public static void printUsage() {
        System.err.println();
        System.err.println("USAGE:");
        System.err.println("    java -Xmx2000m -jar CreateSMemDeclarativeAddScript.jar </path/to/file.owl>");
        System.err.println();
        System.err.println("DESCRIPTION:");
        System.err.println("    Create declarative add statements for ontology.");
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

/**
 *
 * @author besmit
 */
class ObjectPropertyNode {

    final String name;
    final Set<String> types;
    final List<SubjectObjectPair> nodes;

    ObjectPropertyNode(String name) {
        this.name = name;
        this.types = new HashSet();
        this.nodes = new LinkedList();
    }

    SubjectObjectPair addNode(String subjectLTI, String objectLTI) {
        SubjectObjectPair pair = new SubjectObjectPair(subjectLTI, objectLTI);
        if (this.nodes.add(pair)) {
            return pair;
        }
        throw new RuntimeException("Failed to add pair");
    }

    boolean addType(String type) {
        return this.types.add(type);
    }

    @Override()
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override()
    public boolean equals(Object o) {
        if (o instanceof String) {
            String otherName = (String) o;
            return otherName.equals(this.name);
        } else if (o instanceof ObjectPropertyNode) {
            ObjectPropertyNode otherNode = (ObjectPropertyNode) o;
            return otherNode.name.equals(this.name);
        }
        throw new RuntimeException("Cannot compare to: " + o.getClass().getSimpleName());
    }
}

/**
 *
 * @author besmit
 */
class SubjectObjectPair {

    final String subjectLTI, objectLTI;

    SubjectObjectPair(String subjectLTI, String objectLTI) {
        this.subjectLTI = subjectLTI;
        this.objectLTI = objectLTI;
    }
}
