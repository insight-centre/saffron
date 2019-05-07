package org.insightcentre.saffron.web.mongodb;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.term.TermExtraction;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

public class MongoDBHandler implements Closeable {

    private final String url;
    private final int port;
    private final String dbName;
    private final String collectionName;
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection runCollection;
    private final MongoCollection topicsCollection;
    private final MongoCollection topicsCorrespondenceCollection;
    private final MongoCollection topicsExtractionCollection;
    private final MongoCollection authorTopicsCollection;
    private final MongoCollection topicsSimilarityCollection;
    private final MongoCollection authorSimilarityCollection;
    private final MongoCollection taxonomyCollection;

    public MongoDBHandler(String url, int port, String dbName, String collectionName) {
        this.url = url;
        this.port = port;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.mongoClient = new MongoClient(url, port);
        this.database = mongoClient.getDatabase(dbName);
        this.runCollection = database.getCollection(collectionName);
        this.topicsCollection = database.getCollection(collectionName + "_topics");
        this.topicsCorrespondenceCollection = database.getCollection(collectionName + "_topics_correspondence");
        this.topicsExtractionCollection = database.getCollection(collectionName + "_topics_extraction");
        this.authorTopicsCollection = database.getCollection(collectionName + "_author_topics");
        this.topicsSimilarityCollection = database.getCollection(collectionName + "_topics_similarity");
        this.authorSimilarityCollection = database.getCollection(collectionName + "_author_similarity");
        this.taxonomyCollection = database.getCollection(collectionName + "_taxonomy");
    }

    public boolean addRun(String id, Date date) {
        Document document = new Document();
        document.put("id", id);
        document.put("run_date", date);
        runCollection.insertOne(document);
        return true;
    }

    public FindIterable<Document>  getAllRuns() {
        FindIterable<Document> docs = runCollection.find();
        return docs;
    }

    public List<DocumentTopic> getRun(String runId) {
        List<DocumentTopic> docs = new ArrayList<DocumentTopic>();
        return docs;
    }


    public List<DocumentTopic> getTopicExtraction(String runId) {
        List<DocumentTopic> docs = new ArrayList<DocumentTopic>();
        return docs;
    }
    public boolean addTopicExtraction(String id, Date date, Set<Topic> res) {
        Document document = new Document();

        res.forEach(name -> {
            document.put("_id", id + "_" + name.topicString);
            document.put("run", id);
            document.put("run_date", date);
            document.put("topic", name.topicString);
            document.put("score", name.score);
            document.put("dbpedia_url", name.dbpedia_url);
            document.put("mvList", name.mvList);
            document.put("occurrences", name.occurrences);
            document.put("matches", name.matches);
            topicsExtractionCollection.insertOne(document);
        });
        return true;
    }



    public boolean addDocumentTopicCorrespondence(String id, Date date, List<DocumentTopic> topics) {
        Document document = new Document();

        topics.forEach(name -> {
            document.put("_id", id + "_" + name.topic_string + "_" + name.document_id);
            document.put("run", id);
            document.put("run_date", date);
            document.put("topic", name.topic_string);
            document.put("acronym", name.acronym);
            document.put("occurences", name.occurrences);
            document.put("pattern", name.pattern);
            document.put("tfidf", name.tfidf);
            document.put("document_id", name.document_id);
            topicsCorrespondenceCollection.insertOne(document);
        });
        return true;
    }

    public List<DocumentTopic> getDocumentTopicCorrespondence(String runId) {
        List<DocumentTopic> docs = new ArrayList<DocumentTopic>();
        return docs;
    }

    public boolean addTopics(String id, Date date, List<Topic> topics) {
        Document document = new Document();

        topics.forEach(name -> {
            document.put("_id", id + "_" + name.topicString);
            document.put("run", id);
            document.put("run_date", date);
            document.put("topic", name.topicString);
            document.put("matches", name.matches);
            document.put("occurences", name.occurrences);
            document.put("score", name.score);
            document.put("topicString", name.topicString);
            document.put("mvList", name.mvList);
            document.put("dbpedia_url", name.dbpedia_url);
            topicsCollection.insertOne(document);
        });



        return true;
    }

    public FindIterable<Document> getTopics(String runId) {
        List<Topic> topics = new ArrayList<Topic>();
        Block<Document> printBlock = new Block<Document>() {
            @Override
            public void apply(final Document document) {
                System.out.println(document.toJson());
            }
        };

        FindIterable<Document> docs = topicsCollection.find(eq("run", runId));

        return docs;
    }

    public FindIterable<Document> deleteTopic(String runId, String topic) {

        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("run", runId);
        updateFields.append("topic", topic);
        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);
        topicsCollection.findOneAndDelete(and(eq("run", runId), (eq("topic", topic))));
        FindIterable<Document> docs = topicsCollection.find(eq("run", runId));

        return docs;
    }

    public boolean addAuthorTopics(String id, Date date, List<Topic> topics) {
        Document document = new Document();

        topics.forEach(name -> {
            document.put("_id", id + "_" + name.topicString);
            document.put("run", id);
            document.put("run_date", date);
            document.put("author_topic", name.topicString);
            document.put("matches", name.matches);
            document.put("occurences", name.occurrences);
            document.put("score", name.score);
            document.put("topicString", name.topicString);
            document.put("mvList", name.mvList);
            document.put("dbpedia_url", name.dbpedia_url);
            authorTopicsCollection.insertOne(document);
        });
        return true;
    }

    public List<AuthorTopic> getAuthorTopics(String runId) {
        List<AuthorTopic> authorTopics = new ArrayList<AuthorTopic>();
        return authorTopics;
    }

    public boolean addTopicsSimilarity(String id, Date date, List<TopicTopic> topicSimilarity) {
        Document document = new Document();

        topicSimilarity.forEach(name -> {
            document.put("_id", id + "_" + name.getTopic1() + "_" + name.getTopic2());
            document.put("run", id);
            document.put("run_date", date);
            document.put("topic1", name.getTopic1());
            document.put("topic2", name.getTopic2());
            document.put("similarity", name.getSimilarity());

            topicsSimilarityCollection.insertOne(document);
        });
        return true;
    }

    public List<TopicTopic> getTopicsSimilarity(String runId) {
        List<TopicTopic> topicSimilarity = new ArrayList<TopicTopic>();
        return topicSimilarity;
    }

    public boolean addAuthorSimilarity(String id, Date date, List<AuthorAuthor> authorSim) {
        Document document = new Document();

        authorSim.forEach(name -> {
            document.put("_id", id + "_" + name.getAuthor1_id() + "_" + name.getAuthor2_id());
            document.put("run", id);
            document.put("run_date", date);
            document.put("author1", name.getAuthor1_id());
            document.put("author2", name.getAuthor2_id());
            document.put("similarity", name.getSimilarity());

            authorSimilarityCollection.insertOne(document);
        });
        return true;
    }

    public List<AuthorAuthor> getAuthorSimilarity(String runId) {
        List<AuthorAuthor> authorSim = new ArrayList<AuthorAuthor>();
        return authorSim;
    }

    public boolean addTaxonomy(String id, Date date, Taxonomy graph) {
        Document document = new Document();
        //graph.getChildren().forEach(name -> {
            document.put("_id", id + "_" + graph.getRoot());
            document.put("run", id);
            document.put("run_date", date);
            document.put("root", graph.getRoot());
            document.put("children", graph.getChildren());


        taxonomyCollection.insertOne(document);
        //});
        return true;
    }

    public Taxonomy getTaxonomy(String runId) {
        Taxonomy graph = new Taxonomy("", 0,0, null);
        return graph;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        mongoClient.close();
    }


}
