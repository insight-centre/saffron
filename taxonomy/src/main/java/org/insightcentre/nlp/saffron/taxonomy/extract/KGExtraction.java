package org.insightcentre.nlp.saffron.taxonomy.extract;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;


import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class KGExtraction {

    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static final org.apache.jena.rdf.model.Model m_model = ModelFactory.createDefaultModel();

    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://saffron.insight-centre.org/ontology#";
    public static final String SKOS = "http://www.w3.org/2004/02/skos/core#";

    public static final Property PARTONOMY = m_model.createProperty( NS + "partonomy" );
    public static final Property TAXONOMY = m_model.createProperty( NS + "taxonomy" );
    public static final Property SYNONYMY = m_model.createProperty( NS + "synonymy" );
    public static final Property PART_OF = m_model.createProperty( NS + "partOf" );
    public static final Property WHOLE_OF = m_model.createProperty( NS + "wholeOf" );
    public static final Property IS_A = m_model.createProperty( SKOS + "broader" );


    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch(UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }



    public static org.apache.jena.rdf.model.Model knowledgeGraphToRDF(org.apache.jena.rdf.model.Model model, String base, KGExtractionUtils kgExtractionUtils) {
        KnowledgeGraph kg = kgExtractionUtils.getKnowledgeGraph();
        if(kg.getTaxonomy() != null) {
            for(Taxonomy taxonomy : kg.getTaxonomy().children) {
                getPartonomies(model, base, taxonomy, kgExtractionUtils);
            }
        } else {
            System.err.println("No taxonomy to extract");
        }
        Taxonomy taxo = kgExtractionUtils.getKnowledgeGraph().getTaxonomy();
        for(Term term : kgExtractionUtils.getTerms()) {
            getSynonyms(model, base, term, kgExtractionUtils);
            getHyponyms(model, base, taxo, term, kgExtractionUtils);
        }
        model.setNsPrefix("foaf", FOAF.NS);
        model.setNsPrefix("saffron", NS);
        model.setNsPrefix("dct", DCTerms.NS);

        return model;

    }


    private static void getPartonomies(org.apache.jena.rdf.model.Model model, String base, Taxonomy taxonomy, KGExtractionUtils kgExtractionUtils) {
        Resource res;
        for(Taxonomy partonomy : kgExtractionUtils.getKnowledgeGraph().getPartonomy().getComponents()) {
            for (Taxonomy child : partonomy.children) {
                if (taxonomy.hasDescendent(child.root)) {
                    res = model.createResource(base == null ? "" : base + "/rdf/term/" + encode(child.root))
                            .addProperty(RDFS.label, child.root);
                    res.addProperty(RDF.type, model.createResource(SKOS + "Concept"));
                    res.addProperty(PART_OF,
                            model.createResource(
                                    base == null ? encode(partonomy.root)
                                            : base + "/rdf/term/" + encode(partonomy.root)));
                    if (taxonomy.hasDescendent(child.root)) {
                        Taxonomy parent = taxonomy.getParent(child.root);
                        if (parent != null)
                            res.addProperty(IS_A,
                                    model.createResource(
                                            base == null ? encode(parent.root)
                                                    : base + "/rdf/term/" + encode(parent.root)));
                    }

                    Collection<Set<String>> synonymy = kgExtractionUtils.getKnowledgeGraph().getSynonymyClusters();
                    for (Set<String> synonm : synonymy) {
                        if (synonm.contains(child.root) ) {
                            List<String> synonymList = new ArrayList<>();
                            synonm.iterator().forEachRemaining(synonymList::add);
                            for (String text : synonymList) {
                                if (!text.equals(child.root))
                                    res.addProperty(SYNONYMY,
                                            model.createResource(base == null ? encode(text)
                                                    : base + "/rdf/term/" + encode(text)));
                            }
                        }
                    }
                }
                res = model.createResource(base == null ? "" : base + "/rdf/term/" + encode(partonomy.root))
                        .addProperty(RDFS.label, partonomy.root);
                res.addProperty(WHOLE_OF, model.createResource(base == null ? encode(child.root)
                        : base + "/rdf/term/" + encode(child.root)));
            }
        }
    }

    public static void getHyponyms(org.apache.jena.rdf.model.Model model, String base, Taxonomy taxo, Term term, KGExtractionUtils kgExtractionUtils) {
        if(taxo.getRoot().equals(term.getString())) {
            model.createResource(base == null ? "" : base + "/rdf/term/" + encode(term.getString()))
                    .addProperty(RDFS.label, term.getString())
                    .addProperty(RDF.type, model.createResource(SKOS + "Concept"));
        }
        Resource res;
        for (TermTerm tt : kgExtractionUtils.getTermByTerm1(term.getString(), null)) {
            Property prop = model.getProperty(term.toString(), tt.getTerm2());
            Property synonym = model.getProperty(base + "/rdf/term/" + encode(term.getString()), "");
            if (taxo.descendent(term.getString()) != null) {
                for (Taxonomy taxonomy : taxo.descendent(term.getString()).children) {
                    if (!model.contains(synonym, prop) ) {
                        res = model.createResource(base == null ? "" : base + "/rdf/term/" + encode(taxonomy.root))
                                .addProperty(RDFS.label, taxonomy.root);
                        res.addProperty(RDF.type, model.createResource(SKOS + "Concept"));
                        res.addProperty(IS_A,
                                model.createResource(
                                        base == null ? encode(term.getString())
                                                : base + "/rdf/term/" + encode(term.getString())));
                    } else {
                        res = model.getResource(base + "/rdf/term/" + encode(taxonomy.root));
                        res.addProperty(IS_A,
                                model.createResource(
                                        base == null ? encode(term.getString())
                                                : base + "/rdf/term/" + encode(term.getString())));
                    }
                }
            }
        }
    }

    private static void getSynonyms(org.apache.jena.rdf.model.Model model, String base, Term term, KGExtractionUtils kgExtractionUtils) {
        Resource res;
        for(TermTerm tt : kgExtractionUtils.getTermByTerm1(term.getString(), null)) {
            Property prop = model.getProperty(term.toString(), tt.getTerm2());
            Property synonym = model.getProperty(base + "/rdf/term/" + encode(term.getString()), "");
            if (!model.contains(synonym, prop)) {
                Collection<Set<String>> synonymy = kgExtractionUtils.getKnowledgeGraph().getSynonymyClusters();
                for(Set<String> synonm : synonymy) {
                    List<String> synonymList = new ArrayList<>();
                    if (synonm.contains(term.getString())) {
                        synonm.iterator().forEachRemaining(synonymList::add);
                        for (String text : synonymList) {
                            if (!text.equals(term.getString())) {
                                res = model.createResource(base == null ? "" : base + "/rdf/term/" +
                                        encode(term.getString()))
                                        .addProperty(RDFS.label, term.getString());
                                res.addProperty(SYNONYMY,
                                        model.createResource(base == null ? encode(text)
                                                : base + "/rdf/term/" + encode(text)));
                            }
                        }
                    }
                }
            }
        }
    }





    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("b", "The base url").withRequiredArg().ofType(String.class);
                    accepts("o", "The output file path").withRequiredArg().ofType(String.class);
                    accepts("d", "The directory with all files from the kg extraction run").withRequiredArg().ofType(String.class);
                    accepts("taxonomy", "Extract only the taxonomy");
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            String kgOutFile = (String) os.valueOf("o");
            if (kgOutFile == null) {
                badOptions(p, "Output file not given");
            }
            String baseUrl = (String) os.valueOf("b");
            if (baseUrl == null) {
                badOptions(p, "Base url not given");
                return;
            }
            String baseDir = (String) os.valueOf("d");
            if (baseDir == null) {
                badOptions(p, "Base dir not given");
                return;
            }
            KGExtractionUtils kgExtractionUtils = KGExtractionUtils.fromDirectory(new File(baseDir));

            org.apache.jena.rdf.model.Model kg = ModelFactory.createDefaultModel();
            if(os.has("taxonomy")) {
                Taxonomy taxonomy = KGExtractionUtils.loadTaxonomy(new File(baseDir));
                for(Term term : kgExtractionUtils.getTerms()) {
                    getHyponyms(kg, baseUrl, taxonomy, term, kgExtractionUtils);
                }
            } else {
                kg = knowledgeGraphToRDF(kg, baseUrl, kgExtractionUtils);
            }
            kg.setNsPrefix("skos", SKOS);
            try(OutputStream out = new FileOutputStream(kgOutFile)) {
                kg.write( out, "RDF/XML" );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception x) {
            x.printStackTrace();
            return;
        }
    }


    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
}
