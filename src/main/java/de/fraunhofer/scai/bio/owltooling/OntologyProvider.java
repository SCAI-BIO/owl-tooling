/**
 * 
 */
package de.fraunhofer.scai.bio.owltooling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Marc Jacobs
 *
 */
public abstract class OntologyProvider {

	@Getter public OWLOntology ontology;
	@Getter private DefaultPrefixManager prefixManager;
	@Getter private OntologyBuilder builder;
	@Getter @Setter private String source;
	
	public abstract Map<IRI, String> getAnnotations();
	public abstract IRI getOntologyIRI();
	public abstract String getOntologyPrefix();
	public abstract void loadFromFile(String filename) throws FileNotFoundException, OWLOntologyStorageException, IOException;

	protected void init() throws OWLOntologyCreationException {

		builder = new OntologyBuilder(getOntologyIRI().toString());
		ontology = builder.getOntology();

		prefixManager = new DefaultPrefixManager(getOntologyIRI().getIRIString());
		prefixManager.setPrefix(getOntologyPrefix(), getOntologyIRI().getIRIString());

		// fill props of ontology
		for(IRI key : getAnnotations().keySet()) {
			builder.addOntologyAnnotation(builder.getAnnotation(key, getAnnotations().get(key)));
		}
	}
	
	public OntologyProvider() throws OWLOntologyCreationException {
		init();
	}

}
