/**
 * 
 */
package de.fraunhofer.scai.bio.owltooling;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

/**
 * @author Marc Jacobs
 * 
 * some utilities to build an RDFmodel from an ontology
 *
 */
public class RDFBuilder {

	public static Model createProvenance(String ontologyIri, String name, String origsource) {
		Model model = ModelFactory.createDefaultModel();

		model.setNsPrefix("prov", "http://www.w3.org/ns/prov#");
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");

		Resource mapping = model.createResource(ontologyIri+"mapping");
		Resource source = model.createResource(origsource);
		Resource mappingSet = model.createResource("http://owltooling.bio.scai.fraunhofer.de/"+name+".rdf");
		Resource owlTooling = model.createResource("http://owltooling.bio.scai.fraunhofer.de");
		Resource fhg = model.createResource("www.scai.fraunhofer.de");

		Calendar cal = Calendar. getInstance();
		cal. setTime(new Date(System.currentTimeMillis()));

		mapping 
		.addProperty(
				model.createProperty(RDF.TYPE.toString()),
				model.createResource("http://www.w3.org/ns/prov#Activity")
				)		
		.addProperty(
				model.createProperty("http://www.w3.org/ns/prov#atTime"),
				model.createTypedLiteral(cal)
				)
		.addProperty(
				model.createProperty("http://www.w3.org/ns/prov#used"),
				source
				)
		.addProperty(
				model.createProperty("http://www.w3.org/ns/prov#generated"),
				mappingSet
				);

		mappingSet
		.addProperty(
				model.createProperty(RDF.TYPE.toString()),
				model.createResource("http://www.w3.org/ns/prov#Entity")
				)		
		.addProperty(
				model.createProperty("http://purl.org/dc/terms/title"),
				model.createLiteral(name)
				)
		.addProperty(
				model.createProperty("http://www.w3.org/ns/prov#wasAttributedTo"),
				owlTooling
				)
		.addProperty(
				model.createProperty("http://www.w3.org/ns/prov#wasDerivedFrom"),
				source
				)
		.addProperty(
				model.createProperty("http://www.w3.org/ns/prov#wasGeneratedBy"),
				mapping
				)
		;		

		owlTooling
		.addProperty(
				model.createProperty(RDF.TYPE.toString()),
				model.createResource("http://www.w3.org/ns/prov#SoftwareAgent")
				)
		.addProperty(
				model.createProperty(RDFS.label.getURI()),
				model.createLiteral("OWL Tooling")
				)
		.addProperty(
				model.createProperty("http://www.w3.org/ns/prov#actedOnBehalfOf"),
				fhg
				)
		;		

		fhg 
		.addProperty(
				model.createProperty(RDF.TYPE.toString()),
				model.createResource("http://www.w3.org/ns/prov#Organization")
				)
		.addProperty(
				model.createProperty("http://xmlns.com/foaf/0.1/name"),
				model.createLiteral("Fraunhofer SCAI")
				)
		;

		return model;
	}

	public static Model mapNamespaces(DefaultPrefixManager pm) {
		Model model = ModelFactory.createDefaultModel();

		for(String prefix : pm.getPrefixName2PrefixMap().keySet()) {
			if(prefix.length()>1) {
				model.setNsPrefix(prefix.replaceAll(":", ""), pm.getPrefixName2PrefixMap().get(prefix));
			}
		}

		return model;
	}

	public static void addSubClass(Model model, OWLClass parent, OWLClass child) {
		model
		.createResource(child.getIRI().toString())
		.addProperty(
				RDFS.subClassOf, 
				model.createResource(parent.getIRI().toString())
				);

	}

	public static void addSubClassAxiom(Model model, OWLSubClassOfAxiom ax, OWLObjectSomeValuesFrom some) {
		model
		.createResource(ax.getSubClass().asOWLClass().getIRI().toString())
		.addProperty(
				model.createProperty(some.getProperty().asOWLObjectProperty().getIRI().toString()), 
				model.createResource(some.getFiller().asOWLClass().getIRI().toString())
				);
	}

	public static void writeModelToFile(String name, String format, Model model) throws IOException {

		if(format.equals("N3")
				|| format.equals("RDF/XML")
				|| format.equals("N-TRIPLE")
				|| format.equals("TURTLE")
				|| format.equals("TTL")
				) {

			FileOutputStream fout=new FileOutputStream("./" + name+".rdf");
			model.write(fout, format);
			fout.close();
		}

		if(format.equals("gephi")) {

			FileWriter fw = null;
			List<String> nodes = new ArrayList<String>();

			try {
				fw = new FileWriter( "./" + name + "_edge_table.csv" );
				fw.write("Source;Target;Label;Class");
				fw.append( System.getProperty("line.separator") );
				
				StmtIterator  iter = model.listStatements();
				while(iter.hasNext()) {
					Statement stmt = iter.nextStatement();
					
					String from = stmt.getSubject().getURI();
					if(!nodes.contains(from)) nodes.add(from);
					
					String to = stmt.getObject().asNode().getURI();
					if(!nodes.contains(to)) nodes.add(to);
					
					fw.write("" + nodes.lastIndexOf(from));
					fw.write(";");
					fw.write("" + nodes.lastIndexOf(to));
					fw.write(";");
					fw.write(model.shortForm(stmt.getPredicate().getURI().split("#")[1]));
					fw.write(";");
					fw.write(model.shortForm(stmt.getPredicate().getURI().split("#")[1]));
					fw.append( System.getProperty("line.separator") );
				}
			}
			catch ( IOException e ) {
				System.err.println( "Konnte Datei nicht erstellen" );
			}
			finally {
				if ( fw != null )
					try { fw.close(); } catch ( IOException e ) { e.printStackTrace(); }
			}

			try {
				fw = new FileWriter( "./" + name + "_node_table.csv" );
				fw.write("Id;Label;Class");
				fw.append( System.getProperty("line.separator") );
				
				for(int i=0; i<nodes.size(); i++) {

					int idx = nodes.get(i).lastIndexOf("/");

					String label = nodes.get(i).substring(idx+1);
					String clazz = nodes.get(i).substring(0,idx);
					
					
					fw.write( ""+i );
					fw.write( ";" );
					fw.write( "\"" + label + "\"" );
					fw.write( ";" );
					fw.write( "\"" + clazz + "\"" );
					
					fw.append( System.getProperty("line.separator") );
				}
			}
			catch ( IOException e ) {
				System.err.println( "Konnte Datei nicht erstellen" );
			}
			finally {
				if ( fw != null )
					try { fw.close(); } catch ( IOException e ) { e.printStackTrace(); }
			}

		}
	}


}
