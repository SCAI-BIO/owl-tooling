/**
 * 
 */
package de.fraunhofer.scai.bio.owltooling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * @author Marc Jacobs
 *
 */
public class OntologBuilderTest {

    @Test
    public void createBuilderTest() {
    	
    	try {
    		
    		String iri = "https://bio.scai.fraunhofer.de/ontology/Test_"; 
    		
			OntologyBuilder bob = new OntologyBuilder(iri);
			
			assertEquals("IRI doesn't match", bob.getOntologyIRI().toString(), iri);
			assertNotNull("ontology is null", bob.getOntology());
			
			assertEquals("IRI doesn't match", bob.getOntology().getOntologyID().getOntologyIRI().get().toString(), iri);
			assertFalse("version is set", bob.getOntology().getOntologyID().getVersionIRI().isPresent());
			
		} catch (OWLOntologyCreationException e) {
			fail("no exception should have been thrown");
		}
    	
    	
    }
}
