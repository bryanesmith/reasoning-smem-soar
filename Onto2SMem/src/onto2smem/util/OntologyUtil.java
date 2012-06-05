/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package onto2smem.util;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class OntologyUtil {
    
    /**
     * 
     */
    public static final String URI_FILE_PREFIX = "file://";
    
    /**
     * <p>Helper method to load ontology model for an OWL file.</p>
     * @param DIR_PATH
     * @return
     */
    public static OntModel getOntologyModel(final String path) {
        
        String thisPath = path;
        
        if (!path.startsWith(URI_FILE_PREFIX)) {
            thisPath = URI_FILE_PREFIX + thisPath;
        }
        
        OntModel ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        ontologyModel.read(thisPath, "RDF/XML-ABBREV");

        return ontologyModel;
    }
}
