package org.insightcentre.saffron.web.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.insightcentre.nlp.saffron.data.Taxonomy;

import java.util.Date;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class MongoUtils {

    public static FindIterable<Document> getTaxonomyFromMongo(String runId, MongoDBHandler handler) {
        return handler.taxonomyCollection.find(eq("id", runId));
    }

    public static FindIterable<Document> getPartonomyFromMongo(String runId, MongoDBHandler handler) {
        return handler.knowledgeGraphCollection.find(eq("id", runId));
    }

    public static FindIterable<Document> getKnowledgeGraphFromMongo(String runId, MongoDBHandler handler) {
        return handler.knowledgeGraphCollection.find(eq("id", runId));
    }

    public static FindIterable getDocs(MongoDBHandler handler) {
        return handler.runCollection.find();
    }

    public static void deleteRunFromMongo(String name, MongoDBHandler handler) {
        handler.runCollection.findOneAndDelete(and(eq("id", name)));
        handler.termsCollection.deleteMany(and(eq("run", name)));
        handler.termsCorrespondenceCollection.deleteMany(and(eq("run", name)));
        handler.termsExtractionCollection.deleteMany(and(eq("run", name)));
        handler.authorTermsCollection.deleteMany(and(eq("run", name)));
        handler.termsSimilarityCollection.deleteMany(and(eq("run", name)));
        handler.authorSimilarityCollection.deleteMany(and(eq("run", name)));
        handler.taxonomyCollection.findOneAndDelete(and(eq("id", name)));
    }

    public static FindIterable getTermsFromMongo(String runId, MongoDBHandler handler) {
        return handler.termsCollection.find(eq("run", runId));
    }

    public static void updateTermAndTaxonomy(String name, Taxonomy finalTaxon, String termString, String status, MongoDBHandler handler) {
        handler.updateTerm(name, termString, status);
        handler.updateTaxonomy(name, finalTaxon);
    }

}
