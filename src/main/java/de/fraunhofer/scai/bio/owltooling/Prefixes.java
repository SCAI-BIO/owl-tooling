package de.fraunhofer.scai.bio.owltooling;

import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.semanticweb.owlapi.model.IRI;

public class Prefixes {
	
	public final static IRI RDFS_LABEL = IRI.create(RDFS.LABEL.toString());
	public final static IRI RDFS_COMMENT = IRI.create(RDFS.COMMENT.toString());

	public final static IRI DC_IDENTIFIER = IRI.create(DC.IDENTIFIER.toString());
	public final static IRI DC_CREATOR = IRI.create(DC.CREATOR.toString());
	public static final IRI DC_SOURCE = IRI.create(DC.SOURCE.toString());

	public final static IRI TERMS_LICENSE = IRI.create(DCTERMS.LICENSE.toString());
	public final static IRI TERMS_DATE = IRI.create(DCTERMS.DATE.toString());
	public final static IRI TERMS_AGENT = IRI.create(DCTERMS.AGENT.toString());

	public final static IRI SKOS_ALT = IRI.create(SKOS.ALT_LABEL.toString());
	public final static IRI SKOS_PREF = IRI.create(SKOS.PREF_LABEL.toString());
	public final static IRI SKOS_DEF = IRI.create(SKOS.DEFINITION.toString());

	public final static IRI SKOS_EXAKT = IRI.create(SKOS.EXACT_MATCH.toString());
	public final static IRI SKOS_BROAD = IRI.create(SKOS.BROAD_MATCH.toString());
	public final static IRI SKOS_RELATED = IRI.create(SKOS.RELATED_MATCH.toString());
	public final static IRI SKOS_CLOSE = IRI.create(SKOS.CLOSE_MATCH.toString());
	public final static IRI SKOS_EXACT = IRI.create(SKOS.EXACT_MATCH.toString());
	public final static IRI SKOS_NARROW = IRI.create(SKOS.NARROW_MATCH.toString());

}
