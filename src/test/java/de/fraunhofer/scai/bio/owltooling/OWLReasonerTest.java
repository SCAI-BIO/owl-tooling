/**
 * 
 */
package de.fraunhofer.scai.bio.owltooling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 * @author Marc Jacobs
 *
 */
public class OWLReasonerTest {

	OWLOntology ontology = null;

	@Before
	public void initOntology() throws OWLOntologyCreationException {
		
		File ontologyFile = new File("./src/test/resources/pizza.owl");
		ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);
	}

    @Test
    public void createReasonerTest() {

    	assertNotNull("reasoner is null", ontology);
    	    	
    	OWLReasoner reasoner = OntologyReasoner.createReasoner(ontology, "HERMIT");
    	
    	assertNotNull("reasoner is null", reasoner);
    	assertEquals("wrong reasoner name", "HermiT", reasoner.getReasonerName() ); 
    	assertEquals("wrong reasoner version", "1.4.1.513", reasoner.getReasonerVersion().toString() );
    	
    	assertEquals("wrong ontology loader", ontology, reasoner.getRootOntology());

    }

    @Test
    public void checkConsistentTest() {
    	OWLReasoner reasoner = OntologyReasoner.createReasoner(ontology, "HERMIT");
    	assertTrue("is not consistent", OntologyReasoner.checkConsistent(reasoner));
    }
}
