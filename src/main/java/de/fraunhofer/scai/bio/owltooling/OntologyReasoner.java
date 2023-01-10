package de.fraunhofer.scai.bio.owltooling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OntologyReasoner {

    public static Set<String> checkHierarchy(OWLReasoner reasoner, DefaultPrefixManager pm, OWLOntology ontology, int maxDepth) {
        Model model = ModelFactory.createDefaultModel();
        Set<String> cycles = new TreeSet<String>();
        
        Node<OWLClass> topNode = reasoner.getTopClassNode();		
        int i = printHierarchy(topNode, reasoner, 0, pm, ontology, maxDepth, model, cycles, 0);

        log.info("    {} classes in hierarchy up to depth {}.\n", i, maxDepth);

        return cycles;
    }

    public static Set<String> checkHierarchy(OWLReasoner reasoner, IRI rootIRI, DefaultPrefixManager pm, OWLOntology ontology, int maxDepth) {
        Model model = ModelFactory.createDefaultModel();
        Set<String> cycles = new TreeSet<String>();

        OWLClass root = ontology.entitiesInSignature(rootIRI).findFirst().get().asOWLClass();		
        Node<OWLClass> topNode = reasoner.getEquivalentClasses(root);		
        int i = printHierarchy(topNode, reasoner, 0, pm, ontology, maxDepth, model, cycles, 0);

        log.info("    " + i + " classes in hierarchy" + (maxDepth>0 ? " up to depth " + maxDepth : "") + ".\n");

        return cycles;
    }


    public static OWLClass getOWLClassForIri(OWLOntologyManager m, IRI parentIri) {
        OWLDataFactory fac = m.getOWLDataFactory();
        // Get a reference to the vegetarian class so that we can as the
        // reasoner about it. The full IRI of this class happens to be:
        // <http://owl.man.ac.uk/2005/07/sssw/people#vegetarian>
        return fac.getOWLClass(parentIri);
    }

    public static boolean checkIsInTree(OWLClass clazz, OWLClass root, OWLReasoner reasoner) {

        NodeSet<OWLClass> subClses = reasoner.getSubClasses(root, false);
        return subClses.containsEntity(clazz);
    }

    public static List<OWLClass> checkSuperClasses(OWLClass clazz, OWLReasoner reasoner) {

        // Now use the reasoner to obtain the subclasses of vegetarian. We can
        // ask for the direct subclasses of vegetarian or all of the (proper)
        // subclasses of vegetarian. In this case we just want the direct ones
        // (which we specify by the "true" flag).
        NodeSet<OWLClass> superClses = reasoner.getSuperClasses(clazz, true);
        // The reasoner returns a NodeSet, which represents a set of Nodes. Each
        // node in the set represents a subclass of vegetarian pizza. A node of
        // classes contains classes, where each class in the node is equivalent.
        // For example, if we asked for the subclasses of some class A and got
        // back a NodeSet containing two nodes {B, C} and {D}, then A would have
        // two proper subclasses. One of these subclasses would be equivalent to
        // the class D, and the other would be the class that is equivalent to
        // class B and class C. In this case, we don't particularly care about
        // the equivalences, so we will flatten this set of sets and print the
        // result
        log.info(" >> Superclasses of: " + clazz.getIRI());        
        superClses.entities().forEach(cls -> log.info("    " + cls));        
        log.info("\n");

        return superClses.entities().collect(Collectors.toList());
    }

    public static List<OWLClass> checkSubClasses(OWLClass clazz, OWLReasoner reasoner) {
        return checkSubClasses(clazz, reasoner, true);
    }

    public static List<OWLClass> checkSubClasses(OWLClass clazz, OWLReasoner reasoner, boolean print) {

        // Now use the reasoner to obtain the subclasses of vegetarian. We can
        // ask for the direct subclasses of vegetarian or all of the (proper)
        // subclasses of vegetarian. In this case we just want the direct ones
        // (which we specify by the "true" flag).
        NodeSet<OWLClass> subClses = reasoner.getSubClasses(clazz, true);
        // The reasoner returns a NodeSet, which represents a set of Nodes. Each
        // node in the set represents a subclass of vegetarian pizza. A node of
        // classes contains classes, where each class in the node is equivalent.
        // For example, if we asked for the subclasses of some class A and got
        // back a NodeSet containing two nodes {B, C} and {D}, then A would have
        // two proper subclasses. One of these subclasses would be equivalent to
        // the class D, and the other would be the class that is equivalent to
        // class B and class C. In this case, we don't particularly care about
        // the equivalences, so we will flatten this set of sets and print the
        // result
        if(print) {
            log.info(" >> Subclasses of: " + clazz.getIRI());        
            subClses.entities().forEach(cls -> log.info("    " + cls));        
            log.info("\n");
        }

        return subClses.entities().collect(Collectors.toList());
    }

    public static Set<OWLClass> checkUnsatisfiable(OWLReasoner reasoner) {
        // We can easily get a list of unsatisfiable classes. (A class is
        // unsatisfiable if it can't possibly have any instances). Note that the
        // getUnsatisfiableClasses method is really just a convenience method
        // for obtaining the classes that are equivalent to owl:Nothing. In our
        // case there should be just one unsatisfiable class - "mad_cow" We ask
        // the reasoner for the unsatisfiable classes, which returns the bottom
        // node in the class hierarchy (an unsatisfiable class is a subclass of
        // every class).
        Node<OWLClass> bottomNode = reasoner.getUnsatisfiableClasses();
        // This node contains owl:Nothing and all the classes that are
        // equivalent to owl:Nothing - i.e. the unsatisfiable classes. We just
        // want to print out the unsatisfiable classes excluding owl:Nothing,
        // and we can used a convenience method on the node to get these
        Set<OWLClass> unsatisfiable = bottomNode.getEntitiesMinusBottom();
        if (!unsatisfiable.isEmpty()) {
            log.info(" >> The following classes are unsatisfiable: ");
            for (OWLClass cls : unsatisfiable) {
                log.info("    " + cls);
            }
        } else {
            log.info(" >> There are no unsatisfiable classes");
        }
        log.info("\n");

        return unsatisfiable;
    }

    public static OWLReasoner createReasoner(OWLOntology ontology, String name) {
        // We need to create an instance of OWLReasoner. An OWLReasoner provides
        // the basic query functionality that we need, for example the ability
        // obtain the subclasses of a class etc. To do this we use a reasoner
        // factory. Create a reasoner factory. In this case, we will use HermiT,
        // but we could also use FaCT++ (http://code.google.com/p/factplusplus/)
        // or Pellet(http://clarkparsia.com/pellet) Note that (as of 03 Feb
        // 2010) FaCT++ and Pellet OWL API 3.0.0 compatible libraries are
        // expected to be available in the near future). For now, we'll use
        // HermiT HermiT can be downloaded from http://hermit-reasoner.com Make
        // sure you get the HermiT library and add it to your class path. You
        // can then instantiate the HermiT reasoner factory: Comment out the
        // first line below and uncomment the second line below to instantiate
        // the HermiT reasoner factory. You'll also need to import the
        // org.semanticweb.HermiT.Reasoner package.

        //1) Using Factory : [org.semanticweb.HermiT.ReasonerFactory]
        // ReasonerFactory reasonerFactory = new ReasonerFactory();

        //OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        // OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
        // We'll now create an instance of an OWLReasoner (the implementation
        // being provided by HermiT as we're using the HermiT reasoner factory).
        // The are two categories of reasoner, Buffering and NonBuffering. In
        // our case, we'll create the buffering reasoner, which is the default
        // kind of reasoner. We'll also attach a progress monitor to the
        // reasoner. To do this we set up a configuration that knows about a
        // progress monitor. Create a console progress monitor. This will print
        // the reasoner progress out to the console.
        // ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        // Specify the progress monitor via a configuration. We could also
        // specify other setup parameters in the configuration, and different
        // reasoners may accept their own defined parameters this way.
        // OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
        // Create a reasoner that will reason over our ontology and its imports
        // closure. Pass in the configuration.
        // OWLReasoner reasoner =  reasonerFactory.createReasoner(ontology, config);

        OWLReasonerConfiguration config = new Configuration();
        OWLReasoner reasoner = null;

        if(name.equals("HERMIT")) {
            reasoner = new Reasoner((Configuration) config, ontology);
        }
        if(name.equals("STRUCTURAL")) {
            OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
            ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
            config = new SimpleConfiguration(progressMonitor);
            reasoner =  reasonerFactory.createReasoner(ontology, config);
        }

        log.info(reasoner.getReasonerName() + " (" + reasoner.getReasonerVersion() + ")");

        // Ask the reasoner to do all the necessary work now        
        reasoner.precomputeInferences();
        return reasoner;
    }

    public static boolean checkConsistent(OWLReasoner reasoner) {
        // We can determine if the ontology is actually consistent (in this
        // case, it should be).
        boolean consistent = reasoner.isConsistent();
        log.info(" >> Consistent: " + consistent);
        log.info("\n");
        return consistent;
    }


    public static List<OWLClass> getBottomClasses(OWLReasoner reasoner, DefaultPrefixManager pm, OWLOntology ontology, OWLClass root, boolean details) {		
        List<OWLClass> leafs = new ArrayList<OWLClass>();

        Node<OWLClass> bottomNode = reasoner.getBottomClassNode();		

        for (Node<OWLClass> parent : reasoner.getSuperClasses(bottomNode.getRepresentativeElement(), true)) {
            if(root != null 
                    && !checkIsInTree(parent.getRepresentativeElement(), root, reasoner)) continue;

            if(details) { printNode(parent, pm, ontology, null); }
            leafs.add(parent.getRepresentativeElement());
        }

        return leafs;
    }


    public static List<OWLClass> getRootClasses(OWLReasoner reasoner, DefaultPrefixManager pm, OWLOntology ontology, IRI skipNode, boolean details) {		
        List<OWLClass> roots = new ArrayList<OWLClass>();

        Node<OWLClass> topNode = reasoner.getTopClassNode();		

        for (Node<OWLClass> child : reasoner.getSubClasses(topNode.getRepresentativeElement(), true)) {
            if(skipNode != null 
                    && child.getRepresentativeElement().getIRI().equals(skipNode)) continue;

            if(details) { printNode(child, pm, ontology, null); }
            roots.add(child.getRepresentativeElement());
        }

        return roots;
    }

    public static List<TMOntology> getTextMiningOntologies(OWLReasoner reasoner, DefaultPrefixManager pm, OWLOntology ontology, boolean skipBFO, boolean details, String prefix) throws OWLOntologyCreationException {

        log.info(" >> collecting text mining bins...");	

        List<TMOntology> bins = new ArrayList<TMOntology>();

        IRI skipNode = null;
        if(skipBFO) {   // root node of BFO ontology
            skipNode = IRI.create("http://purl.obolibrary.org/obo/BFO_0000001");
        }

        for(OWLClass root: getRootClasses(reasoner, pm, ontology, skipNode, false)) {

            String label = getLabelOfClass(ontology, root);

            if(label != null) {
                label = label.split("@")[0];

                TMOntology tmo = new TMOntology("TM", label.toUpperCase());
                tmo.getBuilder().addClassWithSuperClass(root.getIRI(), OWLRDFVocabulary.OWL_THING.getIRI());
                bins.add(tmo);

                ontology.annotationAssertionAxioms(root.getIRI())
                .forEach(axiom -> {
                    tmo.getBuilder().getManager().applyChange(new AddAxiom(tmo.getOntology(), axiom));
                });

                addSubclasses(reasoner, root, ontology, tmo);
            }
        }

        return bins;
    }

    public static List<TMOntology> getLanguageOntologies(DefaultPrefixManager pm, OWLOntology ontology, boolean details, String name) throws OWLOntologyCreationException {

        String oid = ontology.getOntologyID().getOntologyIRI().get().getIRIString();

        String[] languages = printLanguages(ontology, pm, details);

        List<TMOntology> bins = new ArrayList<TMOntology>();

        for (String lang : languages) {
            log.info("    working on {}...", lang);

            TMOntology tmo = new TMOntology("lang-"+lang, name, false);

            tmo.getBuilder().addOntologyAnnotation(tmo.getBuilder().getAnnotation(pm.getIRI("<http://purl.org/dc/terms/isVersionOf>"), oid));
            tmo.getBuilder().addOntologyAnnotation(tmo.getBuilder().getAnnotation(pm.getIRI("<http://purl.org/dc/terms/language>"), lang));
            tmo.getBuilder().addOntologyAnnotation(tmo.getBuilder().getAnnotation(pm.getIRI("<http://purl.org/dc/terms/provenance"), "filtered for lang:"+lang));

            bins.add(tmo);

            tmo.getOntology().add(ontology.getAxioms());
            filterLabelLanguageTags(tmo.getOntology(), lang, true);

            log.info("    lang:{} # axioms:{}", lang, tmo.getOntology().getAxiomCount());
        }
        return bins;
    }

    public static void filterLabelLanguageTags(OWLOntology ontology, String lang, boolean includeEmpty) {
        Set<OWLClass> classes = ontology.classesInSignature().collect(Collectors.toSet());

        // check all classes and remove all annotations except 1
        for(OWLClass clazz : classes) {

            // collect all labels of class
            Set<OWLAnnotationAssertionAxiom> annotations = ontology.annotationAssertionAxioms(clazz.getIRI())
                    .filter(annotation -> annotation.getProperty().toString().equals("rdfs:label")).collect(Collectors.toSet());

            String label = null;
            OWLAxiom toBeKept = null;

            // check rdfs:label languages
            for(OWLAnnotationAssertionAxiom annotation : annotations ) {
                // correct one
                if(annotation.getValue().asLiteral().get().hasLang() &&
                        lang.equals(annotation.getValue().asLiteral().get().getLang())) {
                    label = annotation.getValue().asLiteral().get().getLiteral();
                    toBeKept = annotation;
                }

                // only an undefined one
                if(!annotation.getValue().asLiteral().get().hasLang() && label == null) {
                    label = annotation.getValue().asLiteral().get().getLiteral();                    
                    toBeKept = annotation;
                }
            }

            if(toBeKept !=  null) { annotations.remove(toBeKept); }
            ontology.remove(annotations);

            // check all language tags 
            List<OWLAxiom> toKeepAll = new ArrayList<OWLAxiom>();
            annotations = ontology.getAnnotationAssertionAxioms(clazz.getIRI());

            for(OWLAnnotationAssertionAxiom annotation : annotations ) {
                // correct one
                if(annotation.getValue().asLiteral().isPresent()) {
                    if(annotation.getValue().asLiteral().get().hasLang() &&
                            lang.equals(annotation.getValue().asLiteral().get().getLang())) {
                        toKeepAll.add(annotation);
                    }
                    if(!annotation.getValue().asLiteral().get().hasLang() &&
                            lang.equals("und")) {
                        toKeepAll.add(annotation);
                    }
                }
            }

            annotations.removeAll(toKeepAll); 
            ontology.remove(annotations);
        }
    }


    public static void addSubclasses(OWLReasoner reasoner, OWLClass root, OWLOntology ontology, OntologyProvider tmo) {
        NodeSet<OWLClass> subClses = reasoner.getSubClasses(root, true);

        for(Node<OWLClass> subClass :  subClses) {
            OWLClass clazz = subClass.getRepresentativeElement();
            if(clazz.getIRI().equals(OWLRDFVocabulary.OWL_NOTHING.getIRI())) {
                continue;
            }

            tmo.getBuilder().addClassWithSuperClass(clazz.getIRI(), root.getIRI());

            ontology.annotationAssertionAxioms(clazz.getIRI())
            .forEach(axiom -> {
                tmo.getBuilder().getManager().applyChange(new AddAxiom(tmo.getOntology(), axiom));
            });

            addSubclasses(reasoner, clazz, ontology, tmo);
        }
    }

    public static Map<String, List<String>> getTextMiningBins(OWLReasoner reasoner, DefaultPrefixManager pm, OWLOntology ontology, IRI skipNode, boolean details) {

        log.info(" >> collecting text mining bins...");	

        Map<String, List<String>> bins = new HashMap<String, List<String>>();

        for(OWLClass root: getRootClasses(reasoner, pm, ontology, skipNode, false)) {
            NodeSet<OWLClass> subClses = reasoner.getSubClasses(root, true);

            for(Node<OWLClass> subClass :  subClses) {
                String label = getLabelOfClass(ontology, root);

                if(label != null) {
                    label = label.split("@")[0];
                    if(!bins.containsKey(label)) {
                        bins.put(label, new ArrayList<String>());
                    }
                    String concept = pm.getShortForm(subClass.getRepresentativeElement()).replaceAll(":_", ":");
                    if(!concept.contains("owl:")) {
                        bins.get(label).add(concept);
                    }
                }
            }
        }

        if(details == true) {
            for(String key : bins.keySet()) {
                System.out.print(key + ": ");
                log.info(bins.get(key).toString());
            }
        }

        return bins;
    }

    public static int printHierarchy(Node<OWLClass> parent, OWLReasoner reasoner, int depth, DefaultPrefixManager pm, OWLOntology ontology, int maxDepth, Model model, Set<String> cycles, int i) {
        // We don't want to print out the bottom node (containing owl:Nothing
        // and unsatisfiable classes) because this would appear as a leaf node
        // everywhere
        if (parent.isBottomNode() || (depth>=maxDepth && maxDepth>0)) {
            return i;
        }
        // Print an indent to denote parent-child relationships
        printIndent(depth);
        // Now print the node (containing the child classes)
        printNode(parent, pm, ontology, cycles);
        i++;

        for (Node<OWLClass> child : reasoner.getSubClasses(
                parent.getRepresentativeElement(), true)) {
            // Recurse to do the children. Note that we don't have to worry
            // about cycles as there are non in the inferred class hierarchy
            // graph - a cycle gets collapsed into a single node since each
            // class in the cycle is equivalent.

            if(!child.isBottomNode()) {
                RDFBuilder.addSubClass(model, parent.entities().findFirst().get(), child.entities().findFirst().get());
            }

            i = printHierarchy(child, reasoner, depth + 1, pm, ontology, maxDepth, model, cycles, i);
        }

        return i;
    }

    public static void printIndent(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("----");
        }
    }

    public static void printClazz(OWLClass clazz, DefaultPrefixManager pm, OWLOntology ontology) {
        if(pm != null) 	{ System.out.print(pm.getShortForm(clazz)); }
        else 			{ System.out.print(clazz.getIRI().toString()); }					

        System.out.print(printLabelsOfClass(ontology, clazz));
        System.out.print(" ");
    }

    public static void printNode(Node<OWLClass> node, DefaultPrefixManager pm, OWLOntology ontology, Set<String> cycles) {        
        // Print out a node as a list of class names in curly brackets
        System.out.print("{ ");

        if(node.getSize() > 1 && cycles != null) {
            StringBuilder sb = new StringBuilder();
            node.entities().forEach(
                    cls -> {
                        if(pm != null)  { sb.append(pm.getShortForm(cls)); }
                        else            { sb.append(cls.getIRI().toString()); }
                        sb.append(" ");
                    }
                    );
            cycles.add(sb.delete(sb.length()-1, sb.length()).toString());
        }
        
        node.entities().forEach(
                cls -> printClazz(cls, pm, ontology)
                );        
        System.out.println("}");
    }


    public static String getLabelOfClass(OWLOntology ontology, OWLClass cls) {
        return getLabelOfClass(ontology, cls, null);
    }


    public static String getDefinitionOfClass(OWLOntology ontology, OWLClass cls, String language) {
        return getAnnotationOfClass(ontology, cls, "rdf:Description", language);
    }

    public static String getLabelOfClass(OWLOntology ontology, OWLClass cls, String language) {
        return getAnnotationOfClass(ontology, cls, "rdfs:label", language);
    }

    public static String getAnnotationOfClass(OWLOntology ontology, OWLClass cls, String property, String language) {

        Optional<OWLAnnotationAssertionAxiom> axiom = null;

        if(language == null) {
            axiom = ontology.annotationAssertionAxioms(cls.getIRI())
                    .filter(annotation -> annotation.getProperty().toString().equals(property))
                    .findFirst();
        } else {
            axiom = ontology.annotationAssertionAxioms(cls.getIRI())
                    .filter(annotation -> annotation.getProperty().toString().equals(property)
                            && annotation.getValue().asLiteral().isPresent()
                            && annotation.getValue().asLiteral().get().hasLang()
                            && annotation.getValue().asLiteral().get().getLang().startsWith(language))
                    .findFirst();
        }

        if(axiom.isPresent()) return axiom.get().getValue().asLiteral().get().getLiteral().replaceAll("\n", "");
        return null;
    }

    public static String printLabelsOfClass(OWLOntology ontology, OWLClass cls) {
        return printLabelsOfClass(ontology, cls, null);
    }

    public static String printLabelsOfClass(OWLOntology ontology, OWLClass cls, String language) {
        StringBuilder sb = new StringBuilder();

        ontology.annotationAssertionAxioms(cls.getIRI())
        .filter(annotation -> annotation.getProperty().toString().equals("rdfs:label"))
        .forEachOrdered(annotation -> {

            if(language == null || 
                    (annotation.getValue().asLiteral().isPresent() 
                            && annotation.getValue().asLiteral().get().hasLang()
                            && annotation.getValue().asLiteral().get().getLang().startsWith(language)
                            )
                    ) {

                sb.append((" " + annotation.getValue().toString().replaceAll("\n", "")));
            }
        });

        return sb.toString();
    }


    public static Model checkMappings(OWLOntology ontology, DefaultPrefixManager pm, String ontologyIri, String name, String origsource, boolean details) {
        log.info(" >> Checking mappings...");

        Model model = RDFBuilder.mapNamespaces(pm);
        model.union(
                RDFBuilder.createProvenance(ontologyIri, name, origsource)
                );

        // model.getNsPrefixMap();		

        AtomicInteger i= new AtomicInteger(0);
        ontology.axioms(AxiomType.SUBCLASS_OF)
        .filter(ax -> ax.getSuperClass() instanceof OWLObjectSomeValuesFrom)
        .forEach(ax -> {

            OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) ax.getSuperClass();

            if(some.getProperty().asOWLObjectProperty().getIRI().toString().contains("Match") ) {
                if(details) {
                    StringBuilder sb = new StringBuilder("    ");
                    sb.append(printShortForm(pm, ax.getSubClass().asOWLClass()));
                    sb.append(" ");
                    sb.append(pm.getShortForm(some.getProperty().asOWLObjectProperty().getIRI()) + " ");
                    sb.append(printShortForm(pm, some.getFiller().asOWLClass()));
                    sb.append("\n");
                    log.info(sb.toString());
                }

                RDFBuilder.addSubClassAxiom(model, ax, some);
                i.incrementAndGet();
            }
        }
                );

        if(details) log.info("\n");
        log.info( "    " + i.toString() + " mappings found.\n");

        return model;
    }

    public static String[] printLanguages(OWLOntology ontology, DefaultPrefixManager pm, boolean details) {
        return checkLanguages(ontology, pm, details).toArray(new String[0]);
    }
    
    public static Set<String> checkLanguages(OWLOntology ontology, DefaultPrefixManager pm, boolean details) {

        log.info(" >> checking languages...");
        Set<String> languages = new TreeSet<String>();

        Map<String, Integer>counts = new HashMap<String, Integer>();

        Set<OWLClass> classes = ontology.classesInSignature().collect(Collectors.toSet());

        // building index
        for(OWLClass clazz : classes) {
            Set<OWLAnnotationAssertionAxiom> annotations = ontology.annotationAssertionAxioms(clazz.getIRI())
                    .filter(annotation -> annotation.getProperty().toString().equals("rdfs:label")).collect(Collectors.toSet());

            for(OWLAnnotationAssertionAxiom annotation : annotations ) {
                String language = annotation.getValue().asLiteral().get().getLang();

                if(language.isEmpty()) {
                    language = "und";
                }
                languages.add( language );

                if(details) {
                    if (!counts.containsKey(language)) {
                        counts.put(language, 0);
                    }
                    counts.put(language, counts.get(language)+1);
                }
            }
        }

        log.info("    languages found {} in {} annotations"
                + "", languages, classes.size());
        if (details) {
            log.info("    {}", counts);
        }
        return languages;
    }

    public static List<OWLClass> checkAnnotations(OWLOntology ontology, DefaultPrefixManager pm, String[] languages, boolean details, Object prefLanguage) {
        log.info(" >> Checking missing labels...");

        Set<OWLClass> classes = ontology.classesInSignature().collect(Collectors.toSet());
        List<OWLClass> toBeFixed = null;

        Map<String, Map<OWLClass,Optional<OWLLiteral>>> labels = new HashMap<String, Map<OWLClass, Optional<OWLLiteral>>>();
        for(String lang : languages) {
            labels.put(lang, new HashMap<OWLClass,Optional<OWLLiteral>>());
        }

        // building index
        for(OWLClass clazz : classes) {
            Set<OWLAnnotationAssertionAxiom> annotations = ontology.annotationAssertionAxioms(clazz.getIRI())
                    .filter(annotation -> annotation.getProperty().toString().equals("rdfs:label")).collect(Collectors.toSet());

            for(OWLAnnotationAssertionAxiom annotation : annotations ) {
                String lang = annotation.getValue().asLiteral().get().getLang();

                if(!labels.containsKey(lang)) {
                    labels.put(lang, new HashMap<OWLClass,Optional<OWLLiteral>>());
                }				
                labels.get(lang).put(clazz, annotation.getValue().asLiteral()); 
            }
        }

        // searching languages
        Map<String, Integer> missing = new HashMap<String, Integer>();
        for(String lang : languages) {
            for(OWLClass clazz : classes) {
                if(!labels.get(lang).containsKey(clazz)) {
                    if(details) {
                        StringBuilder sb = new StringBuilder("    ERROR language " + lang + " tag missing for: ");

                        sb.append(printShortForm(pm, clazz));
                        sb.append(printLabelsOfClass(ontology, clazz));

                        log.info(sb.toString());
                    }

                    if(prefLanguage != null && lang.equals(prefLanguage)) {
                        if(toBeFixed == null) { toBeFixed = new ArrayList<OWLClass>(); }
                        toBeFixed.add(clazz);
                    }

                    if(!missing.containsKey(lang)) { missing.put(lang, 0); }
                    missing.put(lang, missing.get(lang)+1);
                }
            }

            if(details) log.info("\n");
            log.info( "    Language tag-" + lang + " missing " + missing.get(lang)  + " of " + ontology.classesInSignature().count() + " labels.");
        }

        if(details) log.info("\n");

        int same = 0;
        for(int i=0; i< languages.length; i++) {
            for(int j=i+1; j< languages.length; j++) {

                for(OWLClass clazz : labels.get(languages[j]).keySet()) {
                    if(labels.get(languages[i]).containsKey(clazz)) {
                        if(labels.get(languages[i]).get(clazz).get().getLiteral() .equals(labels.get(languages[j]).get(clazz).get().getLiteral())) {
                            if(details) {
                                StringBuilder sb = new StringBuilder("    WARN same: ");
                                sb.append(pm.getShortForm(clazz));
                                sb.append(printLabelsOfClass(ontology, clazz));
                                log.info(sb.toString());
                            }
                            same++;
                        }
                    }
                }
            }
        }

        log.info( "    Labels are same between languages: " + same + " of " + ontology.classesInSignature().count() + " labels.\n");

        return toBeFixed;
    }

    private static String printShortForm(DefaultPrefixManager pm, OWLClass clazz) {
        String pre1 = pm.getPrefixIRI(clazz.getIRI());
        if(pre1 != null) {
            String pre2 = pm.getPrefix(pre1.substring(0, pre1.indexOf(":")+1));
            String post = clazz.getIRI().toString().substring(clazz.getIRI().toString().indexOf(pre2)+pre2.length());
            return(pre1.substring(0, pre1.indexOf(":")+1)+post);
        } else {
            return(clazz.getIRI().toString());			
        }
    }


    public static Map<String, Integer> checkNameSpaces(OWLOntology ontology, DefaultPrefixManager pm) {
        log.info(" >> Distribution over name spaces:");

        Set<OWLClass> classes = ontology.classesInSignature().collect(Collectors.toSet());

        Map<String, Integer> nameSpaces = new TreeMap<String, Integer>();
        for(OWLClass clazz : classes) {
            String key = null;
            String iri = clazz.getIRI().toString();

            if(iri.startsWith("https://bio.scai.fraunhofer.de/ontology/")) {
                if(iri.contains("#")) {
                    String prefix = iri.substring(0,iri.indexOf("#")+1);
                    String inputPrefixName = iri.substring(iri.indexOf("ontology/")+9,iri.indexOf("#"))+":";				 
                    pm.setPrefix(inputPrefixName, prefix);
                } else {
                    iri.toString();
                    // https://bio.scai.fraunhofer.de/ontology/0000419
                }

            }

            // https://bio.scai.fraunhofer.de/ontology/epilepsy#focal_cognitive_seizure_with_conduction_dysphasia/aphasia
            // https://bio.scai.fraunhofer.de/ontology/epilepsy#familial_(autosomal_dominant)_focal_epilepsy
            // <https://bio.scai.fraunhofer.de/ontology/epilepsy#14Hz_and_6Hz_positive_spike>
            // https://bio.scai.fraunhofer.de/ontology/epilepsyZung_self-rating_anxiety 1
            // mental_disorder: 1 { https://bio.scai.fraunhofer.de/ontology/mental_disorder# }
            // https://bio.scai.fraunhofer.de/ontology/78410c86_1712_44d7_afa8
            //     epilepsy: 414 { https://bio.scai.fraunhofer.de/ontology/epilepsy# }

            if(iri.startsWith("http://purl.obolibrary.org/obo/")) {				 
                String prefix = iri.substring(0,iri.indexOf("_"));
                String inputPrefixName = iri.substring(iri.indexOf("obo/")+4,iri.indexOf("_"))+":";				 
                pm.setPrefix(inputPrefixName, prefix);
            }

            if(pm.getShortForm(clazz) != null && pm.getShortForm(clazz).length() < clazz.getIRI().toString().length()) {
                key = pm.getShortForm(clazz).split(":")[0]+":";
            } else if (iri.contains("#")) {
                key = iri.split("#")[0];
            } else if (iri.lastIndexOf("_") >0) {
                key = iri.substring(0, clazz.getIRI().toString().lastIndexOf("_"));				
            } else if (iri.lastIndexOf("/") >0) {
                key = iri.substring(0, clazz.getIRI().toString().lastIndexOf("/"));
            }

            if(key != null) {
                if(!nameSpaces.containsKey(key)) {
                    nameSpaces.put(key, 0);
                }
                nameSpaces.put(key, nameSpaces.get(key)+1);
            }
        }

        //		for(String nameSpace: nameSpaces.keySet()) {
        //			StringBuilder sb = new StringBuilder( "    " + nameSpace + " " + nameSpaces.get(nameSpace));
        //			if(pm.getPrefix(nameSpace) != null) {
        //				sb.append(" { " + pm.getPrefix(nameSpace) + " }");
        //			}
        //			log.info(sb.toString());
        //		}

        for(String nameSpace: nameSpaces.keySet()) {
            StringBuilder sb = new StringBuilder();
            if(nameSpace.startsWith("http")) {
                sb.append("ToBeDefined");								
            } else {
                sb.append(nameSpace.substring(0, nameSpace.length()-1));
            }
            sb.append("\t");

            if(pm.getPrefix(nameSpace) != null) {
                sb.append(pm.getPrefix(nameSpace));
            } else {
                sb.append(nameSpace);
            }
            log.info(sb.toString());
        }


        log.info("\n");
        
        return nameSpaces;
    }

}
