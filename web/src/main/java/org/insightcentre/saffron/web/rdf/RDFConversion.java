package org.insightcentre.saffron.web.rdf;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.saffron.web.SaffronDataSource;


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

        for (DocumentTopic dt : data.getTopicByDoc(datasetName, d.id)) {
            res.addProperty(DCTerms.subject, model.createResource(
                    base == null ? "../term/" + encode(dt.getTermString()) 
                            : base + "/rdf/term/" + encode(dt.getTermString())));
        }

        model.setNsPrefix("foaf", FOAF.NS);
        model.setNsPrefix("saffron", SAFFRON.NS);
        model.setNsPrefix("dct", DCTerms.NS);

        return model;
    }

    public static Model topicToRDF(Term t, SaffronDataSource data, String datasetName) {
        Model model = ModelFactory.createDefaultModel();
        return topicToRDF(t, data, datasetName, model, null);
    }
    
    public static Model topicToRDF(Term t, SaffronDataSource data, String datasetName, Model model, String base) {
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
        
        for(AuthorTopic at : data.getAuthorByTopic(datasetName, t.getString())) {
            res.addProperty(SAFFRON.author, 
                    model.createResource(base == null ?
                            "../author/" + encode(at.author_id)
                            : base + "/rdf/author/" + encode(at.author_id)));
        }
        
        for(TopicTopic tt : data.getTopicByTopic1(datasetName, t.getString(), null)) {
            res.addProperty(SKOS.related,
                    model.createResource(
                            base == null ? encode(tt.topic2)
                                    : base + "/rdf/term/" + encode(tt.topic2)));
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
        for(AuthorTopic at : data.getTopicByAuthor(datasetName, author.id)) {
            res.addProperty(SAFFRON.authorTopic, model.createResource(
                    base == null ? "../term/" + encode(at.topic_id) 
                            : base + "/rdf/term/" + encode(at.topic_id)));
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
        for(Term topic : data.getAllTopics(datasetName)) {
            topicToRDF(topic, data, datasetName, model, base);
        }
        return model;
    }
}
