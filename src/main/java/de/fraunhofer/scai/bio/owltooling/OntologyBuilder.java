package de.fraunhofer.scai.bio.owltooling;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * creating an OWLOntology using the owlapi
 * 
 * @author Marc Jacobs
 *
 */
@Slf4j
public class OntologyBuilder {
	
	public final static IRI OLS_LABEL = IRI.create("https://bio.scai.fraunhofer.de/ontology/OlsLabel");

	@Getter public OWLOntology ontology;
	@Getter public OWLOntologyManager manager;
	private OWLDataFactory df;
	@Getter private IRI ontologyIRI;
	
	public OntologyBuilder(String iri) throws OWLOntologyCreationException {
		manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();
		
		ontologyIRI = IRI.create(iri);
		ontology = manager.createOntology(ontologyIRI);
	}
	
	public void addConceptLabel(IRI iri1, String label) {
		addConceptAnnotation(iri1, getAnnotation(Prefixes.RDFS_LABEL, label));		
	}

	public void addConceptAnnotation(IRI iri1, IRI iri2, List<Label> labelList) {
		addConceptAnnotation(iri1, iri2, labelList, null);
	}

	public void addConceptAnnotation(IRI iri1, IRI iri2, List<Label> labelList, String prefix) {

		if(labelList != null && !labelList.isEmpty()) {
			for(Label label : labelList) {
				if(label.getContent()!= null) {
					String displayString = label.getContent().replaceAll("&.+;", "");
					if(prefix != null) {
						displayString = prefix + displayString;
					}

					if(label.getLanguage() != null) {
						// English is preferred
						if(label.getLanguage().startsWith("en") && iri2.toString().equals(SKOS.PREF_LABEL.toString())) {
							addConceptAnnotation(iri1, getAnnotation(OLS_LABEL, displayString));							
						}						
						addConceptAnnotation(iri1, getAnnotation(iri2, displayString, label.getLanguage()));
					} else {
						addConceptAnnotation(iri1, getAnnotation(iri2, displayString));
					}
				}
			}
		}
	}


	public ChangeApplied addOntologyAnnotation(IRI iri, List<Label> desc) {
		if(desc != null && !desc.isEmpty()) {
			for(Label label : desc) {
				if(label.getContent()!= null) {
					if(label.getLanguage() != null) {
						return addOntologyAnnotation(getAnnotation(iri, label.getContent(), label.getLanguage()));
					} else {
						return addOntologyAnnotation(getAnnotation(iri, label.getContent()));
					}
				}
			}
		}
		
		return ChangeApplied.NO_OPERATION;
	}

	public OWLAnnotation getPrefAnnotation(String annotation, String lang) {
		if(annotation == null) return null;
		return getAnnotation(Prefixes.SKOS_PREF, annotation.replaceAll("[{}<>,]", "").replaceAll("&lt;", ""), lang);
	}

	public OWLAnnotation getAnnotation(IRI iri, String annotation, String lang) {
		if(annotation == null || iri == null) return null;
		else if(lang == null) return getAnnotation(iri, annotation);
		else return df.getOWLAnnotation(
				df.getOWLAnnotationProperty(iri), 
				df.getOWLLiteral(annotation, lang.split("-")[0])
				);
	}

	public OWLAnnotation getAnnotation(IRI iri, String annotation) {
		if(annotation == null || iri == null || annotation.isEmpty()) return null;
		else {
			return df.getOWLAnnotation(
					df.getOWLAnnotationProperty(iri), 
					df.getOWLLiteral(annotation)
					);
		}
	}

	public ChangeApplied addOntologyAnnotation(OWLAnnotation annotation) {
		if(annotation != null) {
			return manager.applyChange(new AddOntologyAnnotation(ontology, annotation));
		}
		
		return ChangeApplied.NO_OPERATION;
	}

	public OWLAxiom addConceptAnnotation(IRI conceptIRI, OWLAnnotation annotation) {
		if(conceptIRI != null && annotation != null) {

			// adding synonyms
			if(annotation.getProperty().getIRI().toString().equals(RDFS.LABEL.toString())) { 
				OWLAxiom axiom = df.getOWLAnnotationAssertionAxiom(conceptIRI,  getAnnotation(IRI.create(SKOS.ALT_LABEL.toString()), stripAnnotation(annotation), null)); 
				manager.applyChange(new AddAxiom(ontology,axiom));
			}

			// adding ols display
			if(annotation.getProperty().getIRI().toString().equals(SKOS.PREF_LABEL.toString()) 
					&& getAnnotationLanguage(annotation) != null 
					&& getAnnotationLanguage(annotation).startsWith("en")) {
				OWLAxiom axiom = df.getOWLAnnotationAssertionAxiom(conceptIRI,  getAnnotation(IRI.create(OLS_LABEL.toString()), stripAnnotation(annotation), null)); 
				manager.applyChange(new AddAxiom(ontology,axiom));				
			}

			OWLAxiom axiom = df.getOWLAnnotationAssertionAxiom(conceptIRI, annotation);		
			manager.applyChange(new AddAxiom(ontology,axiom));
			
			return axiom;
		}
		
		return null;
	}

	public String stripAnnotation(OWLAnnotation annotation) {
		return annotation.getValue().toString().split("@")[0].split("\\^\\^")[0].replaceAll("\"", "");
	}

	public String getAnnotationLanguage(OWLAnnotation annotation) {
		if(annotation.getValue().toString().contains("@")) {
			return annotation.getValue().toString().split("@")[1];
		}
		
		return null;
	}

	public String printOntology() throws OWLOntologyStorageException {
		// display in console
		StringDocumentTarget target = new StringDocumentTarget();
		manager.saveOntology(ontology, new RDFXMLDocumentFormat(), target);
		return target.toString();
	}
	
	public OWLClass getOWLClass(String str) {
		return df.getOWLClass(IRI.create(ontologyIRI+str));
	}

	public OWLClass getOWLClass(IRI iri) {
		return df.getOWLClass(iri);
	}

	public OWLClass addClassWithSuperClass(IRI iri, IRI superIRI) {
		OWLClass owlClass = df.getOWLClass(iri);
		OWLClass superClass = df.getOWLClass(superIRI);

		OWLAxiom axiom = df.getOWLSubClassOfAxiom(owlClass, superClass);
		AddAxiom addAxiom = new AddAxiom(ontology, axiom);
		manager.applyChange(addAxiom);

		return owlClass;
	}

	public OWLIndividual addIndividualWithClass(IRI iri, IRI classIRI) {
		OWLIndividual individual = df.getOWLNamedIndividual(iri);
    
		OWLClassAssertionAxiom axiom = df.getOWLClassAssertionAxiom(getOWLClass(classIRI), individual);
		AddAxiom addAxiom = new AddAxiom(ontology, axiom);
		manager.applyChange(addAxiom);

		return individual;
	}
	
	public OWLSubClassOfAxiom addSomeValues(OWLClass owlClass, OWLObjectSomeValuesFrom owlObjectSomeValuesFrom) {		
		OWLSubClassOfAxiom ax = df.getOWLSubClassOfAxiom(owlClass, owlObjectSomeValuesFrom);
		ontology.add(ax);
		return ax;
	}

	public OWLSubClassOfAxiom addSomeValues(OWLClass subjectClass, OWLClass propertyClass, OWLClassExpression objectClass) {
		return addSomeValues(subjectClass, propertyClass.getIRI(), objectClass);
	}
		
	public OWLSubClassOfAxiom addSomeValues(OWLClass subjectClass, IRI propertyIRI, OWLClassExpression objectClass) {
		return addSomeValues(subjectClass,
			df.getOWLObjectSomeValuesFrom(
					df.getOWLObjectProperty(propertyIRI), 
					objectClass)
			);								
	}
	
	public void saveOntology(String name, String format) throws OWLOntologyStorageException {

		if(format.toLowerCase().equals("rdf")) {
			File file = new File(name+".rdf");
			manager.saveOntology(ontology, new RDFXMLDocumentFormat(), IRI.create(file.toURI()));
			log.info("Written " + file.getName());
			
		} else if (format.toLowerCase().equals("ttl")) {
			File file = new File(name+".ttl");
			TurtleDocumentFormat ttlFormat = new TurtleDocumentFormat();
			if (ontology.getNonnullFormat().isPrefixOWLDocumentFormat()) {
				ttlFormat.copyPrefixesFrom(ontology.getNonnullFormat().asPrefixOWLDocumentFormat());
			}
			manager.saveOntology(ontology, ttlFormat, IRI.create(file.toURI()));
			log.info("Written " + file.getName());
			
		} else if (format.toLowerCase().equals("owl")) {
			File file = new File(name+".owl");
			OWLXMLDocumentFormat owlxmlFormat = new OWLXMLDocumentFormat();
			if (ontology.getNonnullFormat().isPrefixOWLDocumentFormat()) {
				owlxmlFormat.copyPrefixesFrom(ontology.getNonnullFormat().asPrefixOWLDocumentFormat());
			}
			manager.saveOntology(ontology, owlxmlFormat, IRI.create(file.toURI()));
			log.info("Written " + file.getName());
		}

	}

	public void setOLSLabel(List<OWLClass> toBeFixed) {
		for(OWLClass clazz : toBeFixed) {
			Set<OWLAnnotationAssertionAxiom> annotations = ontology.annotationAssertionAxioms(clazz.getIRI())
					.filter(annotation -> annotation.getProperty().toString().equals("rdfs:label")).collect(Collectors.toSet());
			
			log.info(clazz.getIRI().toString());
			
			// take first label
			if(!annotations.isEmpty()) {
				addConceptAnnotation(clazz.getIRI(), getAnnotation(OLS_LABEL, 
						annotations.iterator().next().getValue().asLiteral().get().getLiteral()
					));	
			} else {
				log.info("   PROBLEM: " + clazz.getIRI().toString());
			}
		}
	}

	
}
