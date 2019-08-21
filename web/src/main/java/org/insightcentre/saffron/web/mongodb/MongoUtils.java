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

    public static FindIterable getDocs(MongoDBHandler handler) {
        return handler.runCollection.find();
    }

    public static void deleteRunFromMongo(String name, MongoDBHandler handler) {
        handler.runCollection.findOneAndDelete(and(eq("id", name)));
        handler.topicsCollection.deleteMany(and(eq("run", name)));
        handler.topicsCorrespondenceCollection.deleteMany(and(eq("run", name)));
        handler.topicsExtractionCollection.deleteMany(and(eq("run", name)));
        handler.authorTopicsCollection.deleteMany(and(eq("run", name)));
        handler.topicsSimilarityCollection.deleteMany(and(eq("run", name)));
        handler.authorSimilarityCollection.deleteMany(and(eq("run", name)));
        handler.taxonomyCollection.findOneAndDelete(and(eq("id", name)));
    }

    public static FindIterable getTopicsFromMongo(String runId, MongoDBHandler handler) {
        return handler.topicsCollection.find(eq("run", runId));
    }

    public static void updateTopicAndTaxonomy(String name, Taxonomy finalTaxon, String topicString, String status, MongoDBHandler handler) {
        handler.updateTopic(name, topicString, status);
        handler.updateTaxonomy(name, new Date(), finalTaxon);
    }

}
