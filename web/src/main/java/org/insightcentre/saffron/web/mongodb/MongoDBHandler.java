package org.insightcentre.saffron.web.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import org.bson.conversions.Bson;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.saffron.web.SaffronDataSource;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class MongoDBHandler implements SaffronDataSource {

    private final String url;
    private final int port;
    private String dbName;
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
    private final MongoCollection corpusCollection;

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
        this.corpusCollection = database.getCollection(collectionName + "_corpus");
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbName() {
        return this.dbName;
    }

    public boolean addRun(String id, Date date, Configuration config) {
        Gson gson = new Gson();
        String json = gson.toJson(config);
        Document document = new Document();
        document.put("id", id);
        document.put("run_date", date);
        document.put("config", json);
        runCollection.insertOne(document);
        return true;
    }

    public FindIterable<Document>  getAllRuns() {
        FindIterable<Document> docs = runCollection.find();
        return docs;
    }

    public void deleteRun(String name) {
        Document document = new Document();
        document.put("run", name);
        runCollection.findOneAndDelete(and(eq("id", name)));
        topicsCollection.deleteMany(and(eq("run", name)));
        topicsCorrespondenceCollection.deleteMany(and(eq("run", name)));
        topicsExtractionCollection.deleteMany(and(eq("run", name)));
        authorTopicsCollection.deleteMany(and(eq("run", name)));
        topicsSimilarityCollection.deleteMany(and(eq("run", name)));
        authorSimilarityCollection.deleteMany(and(eq("run", name)));
        taxonomyCollection.findOneAndDelete(and(eq("id", name)));
    }

    @Override
    public String getRun(String runId) {
        JSONObject jsonObj = new JSONObject();
        Iterable<org.bson.Document> docs = runCollection.find(and(eq("id", runId)));
        for (org.bson.Document doc : docs) {
            jsonObj = new JSONObject(doc.toJson());

        }
        return jsonObj.toString();
    }


    public FindIterable<Document> getTopicExtraction(String runId) {
        return topicsExtractionCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getTopicExtractionForTopic(String runId, String topicId) {
        return topicsExtractionCollection.find(and(eq("run", runId), eq("topicId", topicId)));
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

    @Override
    public boolean addRun(String id, Date date) {
        return false;
    }

    public FindIterable<Document> getDocumentTopicCorrespondence(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return topicsCorrespondenceCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getDocumentTopicCorrespondenceForTopic(String runId, String topicId) {

        return topicsCorrespondenceCollection.find(and(eq("run", runId), eq("topic", topicId)));
    }


    public FindIterable<Document> getDocumentTopicCorrespondenceForDocument(String runId, String docId) {
        System.out.println("Document = " + docId);
        return topicsCorrespondenceCollection.find(and(eq("run", runId), eq("document_id", docId)));
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
            document.put("status", "none");
            topicsCollection.insertOne(document);
        });



        return true;
    }

    public FindIterable<Document> getTopics(String runId) {
        FindIterable<Document> docs = topicsCollection.find(eq("run", runId));

        return docs;
    }

    public void deleteTopic(String runId, String topic) {

        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("run", runId);
        updateFields.append("topic", topic);
        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);
        topicsCollection.findOneAndDelete(and(eq("run", runId), (eq("topic", topic))));

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

    public FindIterable<Document>  getAuthorTopics(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return authorTopicsCollection.find(and(eq("run", runId)));

    }

    public FindIterable<Document>  getAuthorTopicsForTopic(String runId, String topic) {
        Document document = new Document();
        document.put("run", runId);
        return authorTopicsCollection.find(and(eq("run", runId), eq("author_topic", topic)));

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

    public FindIterable<Document> getTopicsSimilarity(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return topicsSimilarityCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getTopicsSimilarityBetweenTopics(String runId, String topic1, String topic2) {
        Document document = new Document();
        document.put("run", runId);
        return topicsSimilarityCollection.find(and(eq("run", runId), eq("topic1", topic1), eq("topic2", topic2)));
    }

    public FindIterable<Document> getTopicsSimilarityForTopic(String runId, String topic) {
        Document document = new Document();
        document.put("run", runId);
        return topicsSimilarityCollection.find(and(eq("run", runId), eq("topic1", topic)));
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

    public FindIterable<Document> getAuthorSimilarity(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return authorSimilarityCollection.find(and(eq("run", runId)));
    }


    public FindIterable<Document> getAuthorSimilarityForTopic(String runId, String topic1, String topic2) {
        Document document = new Document();
        document.put("run", runId);
        return authorSimilarityCollection.find(and(eq("run", runId), eq("topic1", topic1), eq("topic2", topic2)));
    }

    public boolean addTaxonomy(String id, Date date, Taxonomy graph) {

        ObjectMapper mapper = new ObjectMapper();


        try{
            Document doc = Document.parse( mapper.writeValueAsString(graph) );
            doc.append("id", id);
            doc.append("date", date);
            taxonomyCollection.insertOne(doc);
            return true;

        } catch (Exception e) {
            System.out.println(e);
        }


        return false;

    }

    public boolean updateTaxonomy(String id, Date date, Taxonomy graph) {
        Document doc = new Document();
        doc.append("id", id);
        try {
            taxonomyCollection.findOneAndDelete(doc);
            this.addTaxonomy(id, date, graph);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Failed to reject the topic from the taxonomy " + id);
            return false;
        }

    }


    @Override
    public Taxonomy getTaxonomy(String runId)  {
        FindIterable<Document> docs = taxonomyCollection.find(eq("id", runId));
        Taxonomy graph = new Taxonomy("", 0, 0, "", "", new ArrayList<>(), Status.none);
        for (org.bson.Document doc : docs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            try {
                graph = Taxonomy.fromJsonString(jsonObj.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return graph;
    }


    @Override
    public List<DocumentTopic> getDocTopics(String runId) {
        return null;
    }

    @Override
    public List<String> getTaxoParents(String runId, String topic_string) {
        return null;
    }

    @Override
    public List<TopicAndScore> getTaxoChildrenScored(String runId, String topic_string) {
        return null;
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor1(String runId, String author1) {
        return null;
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor2(String runId, String author1) {
        return null;
    }

    @Override
    public List<Author> authorAuthorToAuthor1(String runId, List<AuthorAuthor> aas) {
        return null;
    }

    @Override
    public List<Author> authorAuthorToAuthor2(String runId, List<AuthorAuthor> aas) {
        return null;
    }

    @Override
    public List<String> getTaxoChildren(String runId, String topic_string) {
        return null;
    }

    @Override
    public List<TopicTopic> getTopicByTopic1(String runId, String topic1, List<String> _ignore) {
        return null;
    }

    @Override
    public List<TopicTopic> getTopicByTopic2(String runId, String topic2) {
        return null;
    }

    @Override
    public List<AuthorTopic> getTopicByAuthor(String runId, String author) {
        return null;
    }

    @Override
    public List<AuthorTopic> getAuthorByTopic(String runId, String topic) {
        return null;
    }

    @Override
    public List<Author> authorTopicsToAuthors(String runId, List<AuthorTopic> ats) {
        return null;
    }

    @Override
    public List<DocumentTopic> getTopicByDoc(String runId, String doc) {
        return null;
    }

    @Override
    public List<org.insightcentre.nlp.saffron.data.Document> getDocByTopic(String runId, String topic) {
        return null;
    }

    @Override
    public Topic getTopic(String runId, String topic) {
        return null;
    }

    @Override
    public List<org.insightcentre.nlp.saffron.data.Document> getDocsByAuthor(String runId, String authorId) {
        return null;
    }

    @Override
    public Collection<String> getTopTopics(String runId, int from, int to) {
        return null;
    }

    @Override
    public Author getAuthor(String runId, String authorId) {
        return null;
    }

    @Override
    public org.insightcentre.nlp.saffron.data.Document getDoc(String runId, String docId) {
        return null;
    }

    @Override
    public DocumentSearcher getSearcher(String runId) {
        return null;
    }

    @Override
    public void setDocTopics(String runId, List<DocumentTopic> docTopics) {

    }

    @Override
    public void setCorpus(String runId, DocumentSearcher corpus) {

    }

    @Override
    public void setCorpus(String runId, JSONObject corpus) {

    }

    @Override
    public void setTopics(String runId, Collection<Topic> _topics) {

    }

    @Override
    public void setAuthorTopics(String runId, Collection<AuthorTopic> authorTopics) {

    }

    @Override
    public void setTopicSim(String runId, List<TopicTopic> topicSim) {

    }

    @Override
    public void setAuthorSim(String runId, List<AuthorAuthor> authorSim) {

    }

    @Override
    public void setTaxonomy(String runId, Taxonomy taxonomy) {

    }

    @Override
    public void remove(String runId) {

    }


    public boolean updateTopic(String id, String topic, String status) {

        try {
            Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("topic", topic));
            Bson update = set("status", status);


            FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
            findOptions.upsert(true);
            findOptions.returnDocument(ReturnDocument.AFTER);

            topicsCollection.findOneAndUpdate(condition, update, findOptions);
            //this.updateTopicSimilarity(id, topic, status);
            return true;
        } catch (Exception e ) {
            e.printStackTrace();
            System.err.println("Failed to reject the topic " + topic + " from the taxonomy " + id);
            return false;
        }

    }



    public boolean updateTopicSimilarity(String id, String topic1, String topic2, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("topic1", topic1),
                Filters.eq("topic2", topic2));
        Bson update = set("status", status);



        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the topic " + topic1 + " from the taxonomy " + id);
            return false;
        }

    }

    public boolean updateAuthorTopicName(String id, String topic, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("author_topic", topic));
        Bson update = combine(set("author_topic", newTopic), set("topicString", newTopic),
                set("originalTopic", topic), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the topic " + topic + " from the taxonomy " + id);
            return false;
        }

    }


    public boolean updateDocumentTopicName(String id, String topic, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("topic", topic));
        Bson update = combine(set("topic", newTopic),
                set("originalTopic", topic), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsCorrespondenceCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the topic " + topic + " from the taxonomy " + id);
            return false;
        }

    }

    public boolean updateTopicSimilarityName(String id, String topic, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("topic1", topic));
        Bson update = combine(set("topic1", newTopic),
                set("originalTopic", topic), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the topic " + topic + " from the taxonomy " + id);
            return false;
        }

    }




    public boolean updateTopicName(String id, String topic, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("topic", topic));
        Bson update = combine(set("topic", newTopic), set("topicString", newTopic), set("originalTopic", topic));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);

        topicsCollection.findOneAndUpdate(condition, update, findOptions);
        return true;
    }

    @Override
    public boolean containsKey(String id) {
        return false;
    }

    @Override
    public boolean isLoaded(String id) {
        return false;
    }

    @Override
    public Iterable<String> runs() {
        return null;
    }

    @Override
    public Taxonomy getTaxoDescendent(String runId, String topicString) {
        return null;
    }

    @Override
    public Iterable<org.insightcentre.nlp.saffron.data.Document> getAllDocuments(String datasetName) {
        return null;
    }

    @Override
    public Iterable<Author> getAllAuthors(String datasetName) {
        return null;
    }

    @Override
    public Iterable<Topic> getAllTopics(String datasetName) {
        return null;
    }

    @Override
    public Date getDate(String doc) {
        return null;
    }

    @Override
    public List<AuthorTopic> getAllAuthorTopics(String name) {
        return null;
    }

    @Override
    public Iterable<DocumentTopic> getDocTopicByTopic(String name, String topicId) {
        return null;
    }

    @Override
    public Iterable<TopicTopic> getAllTopicSimilarities(String name) {
        return null;
    }

    @Override
    public Iterable<TopicTopic> getTopicByTopics(String name, String topic1, String topic2) {
        return null;
    }


    public FindIterable<Document> searchTaxonomy(String id, String term) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("topic", term));

        return topicsCorrespondenceCollection.find(condition);
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


    public boolean addCorpus(String saffronDatasetName, Date date, Corpus corpus) {
        ObjectMapper mapper = new ObjectMapper();


        try{
            Document doc = Document.parse( mapper.writeValueAsString(corpus) );
            doc.append("id", saffronDatasetName);
            doc.append("date", date);
            FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
            findOptions.upsert(true);
            findOptions.returnDocument(ReturnDocument.AFTER);
            if (getCorpusCount(saffronDatasetName) > 0)
                corpusCollection.findOneAndDelete(doc);

            corpusCollection.insertOne(doc);

            return true;

        } catch (Exception e) {
            System.out.println(e);
        }


        return false;
    }

    public long getCorpusCount(String saffronDatasetName) {
        Document document = new Document();
        document.put("id", saffronDatasetName);
        return corpusCollection.count(and(eq("id", saffronDatasetName)));
    }

    public FindIterable<Document> getCorpus(String saffronDatasetName) {
        Document document = new Document();
        document.put("id", saffronDatasetName);
        return corpusCollection.find(and(eq("id", saffronDatasetName)));
    }
}
