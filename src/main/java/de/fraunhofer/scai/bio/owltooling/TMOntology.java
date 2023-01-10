/**
 * 
 */
package de.fraunhofer.scai.bio.owltooling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import lombok.Getter;

/**
 * @author Marc Jacobs
 *
 */
public class TMOntology extends OntologyProvider {

	@Getter String name;
	
	public void setName(String name) {
		this.name = name
			.replaceAll("TM_BIN", "")
			.trim().replaceAll("\\s", "_");
	}

	public TMOntology(String name) throws OWLOntologyCreationException {
		setName(name);
		super.init();
	}

	public TMOntology(String prefix, String label) throws OWLOntologyCreationException {
	    this(prefix, label, true);
	}

	public TMOntology(String prefix, String label, boolean isPrefix) throws OWLOntologyCreationException {
       if(isPrefix) {
           setName(prefix+"_"+label);
           super.init();
       } else {
           setName(label+"_"+prefix);
           super.init();
       }
	}
	
	@Override
	public Map<IRI, String> getAnnotations() {
		Map<IRI, String> annotations = new HashMap<IRI, String>();
		return annotations;
	}

	@Override
	public IRI getOntologyIRI() {
		try {
			if(getName() != null) {
				return IRI.create("https://bio.scai.fraunhofer.de/ontology/" + URLEncoder.encode(getName(), "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return IRI.create("https://bio.scai.fraunhofer.de/ontology/");
	}

	@Override
	public String getOntologyPrefix() {
		return getName()+":";
	}

	@Override
	public void loadFromFile(String filename) throws FileNotFoundException, OWLOntologyStorageException, IOException {
		throw new OWLOntologyStorageException("not implemented yet.");
	}
}
