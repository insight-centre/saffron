package org.insightcentre.saffron.web.rdf;

import java.io.*;
import java.net.URLEncoder;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.saffron.web.SaffronDataSource;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;


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
        for(Term term : data.getAllTerms(datasetName)) {
            termToRDF(term, data, datasetName, model, base);
        }
        return model;
    }


    public static Model knowledgeGraphToRDF(KnowledgeGraph kg, String datasetName) {
        Model model = ModelFactory.createDefaultModel();
        return knowledgeGraphToRDF(kg, datasetName, model);
    }

    public static Model knowledgeGraphToRDF(KnowledgeGraph kg, String datasetName, Model model) {
        Resource res = model.createResource("http://localhost:8080/rdf/knowledgegraph/" + encode(datasetName))
                .addProperty(RDF.type, FOAF.KnowledgeGraph).addProperty(SAFFRON.taxonomy, model.createResource()
                    .addProperty(RDF.value, kg.getTaxonomy().toString()));

        for (Taxonomy taxonomy : kg.getPartonomy().getComponents()) {
            res.addProperty(SAFFRON.partonomy,  model.createResource()
                    .addProperty(RDF.value, taxonomy.toString()));
        }

        for (Set<String> synonmy : kg.getSynonymyClusters()) {
            res.addProperty(SAFFRON.synonmy,
                    model.createResource()
                            .addProperty(RDF.value, synonmy.toString()));
        }

        model.setNsPrefix("foaf", FOAF.NS);
        model.setNsPrefix("saffron", SAFFRON.NS);
        model.setNsPrefix("dct", DCTerms.NS);

        return model;
    }


    public static void main(String[] args) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final MongoDBHandler saffron = new MongoDBHandler();
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("t", "The name of the Saffron knowledge graph").withRequiredArg().ofType(String.class);
                    accepts("o", "The output file").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }


            File kgOutFile = (File) os.valueOf("o");
            if (kgOutFile == null) {
                badOptions(p, "Output file not given");
            }

            String datasetName = (String) os.valueOf("t");
            if (datasetName == null ) {
                badOptions(p, "The data set does not exist");
            }



            final Model kg = knowledgeGraphToRDF(saffron.getKnowledgeGraph(datasetName), datasetName);
            try(OutputStream out = new FileOutputStream("filename.rdf")) {
                kg.write( out, "RDF/XML" );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //mapper.writerWithDefaultPrettyPrinter().writeValue(kgOutFile, kg);
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
