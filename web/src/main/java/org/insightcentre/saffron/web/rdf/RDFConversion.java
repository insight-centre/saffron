package org.insightcentre.saffron.web.rdf;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.insightcentre.nlp.saffron.taxonomy.classifiers.BERTBasedRelationClassifier;
import org.insightcentre.nlp.saffron.taxonomy.search.KGSearch;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;
import org.insightcentre.saffron.web.SaffronDataSource;
import org.insightcentre.saffron.web.SaffronInMemoryDataSource;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;

import static org.insightcentre.nlp.saffron.taxonomy.supervised.Main.loadMap;


/**
 *
 * @author John McCrae
 */
public class RDFConversion {


    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch(UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }


    
    public static Model documentToRDF(Document d, SaffronDataSource data, String datasetName) {
        Model model = ModelFactory.createDefaultModel();
        
        return documentToRDF(d, data, datasetName, model, null);
        
    }
    
    public static Model documentToRDF(Document d, SaffronDataSource data, String datasetName, Model model, String base) {

        Resource res = model.createResource(base == null ? "" : base + "/rdf/doc/" + encode(d.id))
                .addProperty(RDF.type, FOAF.Document);
        if (d.getName() != null) {
            res.addLiteral(RDFS.label, d.getName());
        }
        if (d.url != null) {
            res.addProperty(RDFS.seeAlso, model.createResource(d.url.toString()));
        }
        if (d.mimeType != null) {
            res.addLiteral(DCTerms.type, d.getMimeType());
        }

        for (DocumentTerm dt : data.getTermByDoc(datasetName, d.id)) {
            res.addProperty(DCTerms.subject, model.createResource(
                    base == null ? "../term/" + encode(dt.getTermString()) 
                            : base + "/rdf/term/" + encode(dt.getTermString())));
        }

        model.setNsPrefix("foaf", FOAF.NS);
        model.setNsPrefix("saffron", SAFFRON.NS);
        model.setNsPrefix("dct", DCTerms.NS);

        return model;
    }

    public static Model termToRDF(Term t, SaffronDataSource data, String datasetName) {
        Model model = ModelFactory.createDefaultModel();
        return termToRDF(t, data, datasetName, model, null);
    }
    
    public static Model termToRDF(Term t, SaffronDataSource data, String datasetName, Model model, String base) {
        Resource res = model.createResource(base == null ? "" : base + "/rdf/term/" + encode(t.getString()))
                .addProperty(RDF.type, SKOS.Concept)
                .addProperty(RDFS.label, t.getString())
                .addProperty(SAFFRON.occurrences, model.createTypedLiteral(t.getOccurrences()))
                .addProperty(SAFFRON.matches, model.createTypedLiteral(t.getMatches()))
                .addProperty(SAFFRON.score, model.createTypedLiteral(t.getScore()));
        if (t.getDbpediaUrl() != null) {
            res.addProperty(SKOS.exactMatch, model.createResource(t.getDbpediaUrl().toString()));
        }
        for (Term.MorphologicalVariation mv : t.getMorphologicalVariationList()) {
            res.addProperty(SAFFRON.morphologicalVariant,
                    model.createResource()
                    .addProperty(RDF.value, mv.string)
                    .addProperty(SAFFRON.occurrences,
                            model.createTypedLiteral(mv.occurrences)));
        }
        
        for(AuthorTerm at : data.getAuthorByTerm(datasetName, t.getString())) {
            res.addProperty(SAFFRON.author, 
                    model.createResource(base == null ?
                            "../author/" + encode(at.getAuthorId())
                            : base + "/rdf/author/" + encode(at.getAuthorId())));
        }
        
        for(TermTerm tt : data.getTermByTerm1(datasetName, t.getString(), null)) {
            res.addProperty(SKOS.related,
                    model.createResource(
                            base == null ? encode(tt.getTerm2())
                                    : base + "/rdf/term/" + encode(tt.getTerm2())));
        }

        model.setNsPrefix("foaf", FOAF.NS);
        model.setNsPrefix("saffron", SAFFRON.NS);
        model.setNsPrefix("dct", DCTerms.NS);
        
        return model;
    }
    
    public static Model authorToRdf(Author author, SaffronDataSource data, String datasetName) {
        Model model = ModelFactory.createDefaultModel();
        return authorToRdf(author, data, datasetName, model, null);
    }
    
    public static Model authorToRdf(Author author, SaffronDataSource data, String datasetName, Model model, String base) {

        Resource res = model.createResource(base == null ? "" 
                : base + "/rdf/author/" + encode(author.id))
                .addProperty(RDF.type, FOAF.Person)
                .addLiteral(FOAF.name, author.name);
        if (author.nameVariants != null){
        	for(String variant : author.nameVariants) {
        		res.addLiteral(FOAF.nick, variant);
        	}
        }
        for(AuthorAuthor aa : data.getAuthorSimByAuthor1(datasetName, author.id)) {
            res.addProperty(SAFFRON.relatedAuthor, model.createResource(
                    base == null ? encode(aa.author2_id)
                            : base + "/rdf/author/" + aa.author2_id));
        }
        for(AuthorTerm at : data.getTermByAuthor(datasetName, author.id)) {
            res.addProperty(SAFFRON.authorTerm, model.createResource(
                    base == null ? "../term/" + encode(at.getTermId()) 
                            : base + "/rdf/term/" + encode(at.getTermId())));
        }
        for(Document d : data.getDocsByAuthor(datasetName, author.id)) {
            res.addProperty(FOAF.made, base == null ? 
                    "../doc/" + encode(d.id)
                    : base + "/rdf/doc/" + encode(d.id));
        }
        
        model.setNsPrefix("foaf", FOAF.NS);
        model.setNsPrefix("saffron", SAFFRON.NS);
        model.setNsPrefix("dct", DCTerms.NS);
        
        return model;
        
    }
    
    public static Model allToRdf(String base, SaffronDataSource data, String datasetName) {
        Model model = ModelFactory.createDefaultModel();
        for(Document doc : data.getAllDocuments(datasetName)) {
            documentToRDF(doc, data, datasetName, model, base);
        }
        for(Author auth : data.getAllAuthors(datasetName)) {
            authorToRdf(auth, data, datasetName, model, base);
        }
        knowledgeGraphToRDF(data, datasetName, model, base);
        return model;
    }


    public static Model knowledgeGraphToRDF(SaffronDataSource data, String datasetName, Model model, String base) {
        for(Taxonomy taxonomy : data.getKnowledgeGraph(datasetName).getTaxonomy().children) {
            getPartonomies(data, datasetName, model, base, taxonomy);
        }
        Taxonomy taxo = data.getKnowledgeGraph(datasetName).getTaxonomy();
        for(Term term : data.getAllTerms(datasetName)) {
            getSynonyms(data, datasetName, model, base, term);
            getHyponyms(data, datasetName, model, base, taxo, term);
        }
        model.setNsPrefix("foaf", FOAF.NS);
        model.setNsPrefix("saffron", SAFFRON.NS);
        model.setNsPrefix("dct", DCTerms.NS);

        return model;

    }

    private static void getPartonomies(SaffronDataSource data, String datasetName, Model model, String base, Taxonomy taxonomy) {
        Resource res;
        for(Taxonomy partonomy : data.getKnowledgeGraph(datasetName).getPartonomy().getComponents()) {
            for (Taxonomy child : partonomy.children) {
                if (taxonomy.hasDescendent(child.root)) {
                    res = model.createResource(base == null ? "" : base + "/rdf/term/" + encode(child.root))
                            .addProperty(RDFS.label, child.root);
                    res.addProperty(SAFFRON.partOf,
                            model.createResource(
                                    base == null ? encode(partonomy.root)
                                            : base + "/rdf/term/" + encode(partonomy.root)));
                    if (taxonomy.hasDescendent(child.root)) {
                        Taxonomy parent = taxonomy.getParent(child.root);
                        if (parent != null)
                            res.addProperty(SAFFRON.isA,
                                    model.createResource(
                                            base == null ? encode(parent.root)
                                                    : base + "/rdf/term/" + encode(parent.root)));
                    }

                    Collection<Set<String>> synonymy = data.getKnowledgeGraph(datasetName).getSynonymyClusters();
                    for (Set<String> synonm : synonymy) {
                        if (synonm.contains(child.root) ) {
                            List<String> synonymList = new ArrayList<>();
                            synonm.iterator().forEachRemaining(synonymList::add);
                            for (String text : synonymList) {
                                if (!text.equals(child.root))
                                    res.addProperty(SAFFRON.synonym,
                                        model.createResource(base == null ? encode(text)
                                                : base + "/rdf/term/" + encode(text)));
                            }
                        }
                    }
                }
                res = model.createResource(base == null ? "" : base + "/rdf/term/" + encode(partonomy.root))
                            .addProperty(RDFS.label, partonomy.root);
                res.addProperty(SAFFRON.wholeOf, model.createResource(base == null ? encode(child.root)
                                            : base + "/rdf/term/" + encode(child.root)));
            }
        }
    }

    private static void getHyponyms(SaffronDataSource data, String datasetName, Model model, String base, Taxonomy taxo, Term term) {
        Resource res;
        for (TermTerm tt : data.getTermByTerm1(datasetName, term.getString(), null)) {
            Property prop = model.getProperty(term.toString(), tt.getTerm2());
            Property synonym = model.getProperty(base + "/rdf/term/" + encode(term.getString()), "");
            if (taxo.descendent(term.getString()) != null) {
                for (Taxonomy taxonomy : taxo.descendent(term.getString()).children) {
                    if (!model.contains(synonym, prop) ) {
                            res = model.createResource(base == null ? "" : base + "/rdf/term/" + encode(taxonomy.root));
                            res.addProperty(SAFFRON.isA,
                                    model.createResource(
                                            base == null ? encode(term.getString())
                                                    : base + "/rdf/term/" + encode(term.getString())));
                    } else {
                            res = model.getResource(base + "/rdf/term/" + encode(taxonomy.root));
                            res.addProperty(SAFFRON.isA,
                                    model.createResource(
                                            base == null ? encode(term.getString())
                                                    : base + "/rdf/term/" + encode(term.getString())));
                    }
                }
            }
        }
    }

    private static void getSynonyms(SaffronDataSource data, String datasetName, Model model, String base, Term term) {
        Resource res;
        for(TermTerm tt : data.getTermByTerm1(datasetName, term.getString(), null)) {
            Property prop = model.getProperty(term.toString(), tt.getTerm2());
            Property synonym = model.getProperty(base + "/rdf/term/" + encode(term.getString()), "");
            if (!model.contains(synonym, prop)) {
                Collection<Set<String>> synonymy = data.getKnowledgeGraph(datasetName).getSynonymyClusters();
                for(Set<String> synonm : synonymy) {
                    List<String> synonymList = new ArrayList<>();
                    if (synonm.contains(term.getString())) {
                        synonm.iterator().forEachRemaining(synonymList::add);
                        for (String text : synonymList) {
                            if (!text.equals(term.getString())) {
                                res = model.createResource(base == null ? "" : base + "/rdf/term/" +
                                        encode(term.getString()))
                                        .addProperty(RDFS.label, term.getString());
                                res.addProperty(SAFFRON.synonym,
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
            final SaffronInMemoryDataSource saffron = new SaffronInMemoryDataSource();
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("b", "The base url").withRequiredArg().ofType(String.class);
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("o", "The output file").withRequiredArg().ofType(String.class);
                    accepts("t", "The name of the Saffron knowledge graph").withRequiredArg().ofType(String.class);
                    accepts("d", "The home directory").withRequiredArg().ofType(String.class);
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
            if (os.valueOf("c") == null) {
                badOptions(p, "Configuration is required");
                return;
            }
            String datasetName = (String) os.valueOf("t");
            if (datasetName == null ) {
                badOptions(p, "The data set does not exist");
            }
            String baseUrl = (String) os.valueOf("b");
            if (baseUrl == null) {
                badOptions(p, "Base url not given");
            }
            String baseDir = (String) os.valueOf("d");
            if (baseUrl == null) {
                badOptions(p, "Base dir not given");
            }
            File datasetNameFile;
            File kgOutFileName;
            if(!datasetName.startsWith(baseDir)) {
                datasetNameFile = new File(baseDir + "/" + datasetName);
                kgOutFileName = new File(baseDir + "/" + kgOutFile);
            } else {
                datasetNameFile = new File(datasetName);
                kgOutFileName = new File(datasetName + '/' + kgOutFile);
            }
            int index=datasetName.lastIndexOf('/');
            datasetName = datasetName.substring(index+1,datasetName.length());
            saffron.fromDirectory(datasetNameFile, datasetName);
            Model kg = ModelFactory.createDefaultModel();
            kg = knowledgeGraphToRDF(saffron, datasetName, kg, baseUrl);
            try(OutputStream out = new FileOutputStream(kgOutFileName)) {
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
