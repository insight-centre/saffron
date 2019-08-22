package org.insightcentre.saffron.web.mongodb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
import org.insightcentre.saffron.web.SaffronInMemoryDataSource;
import org.insightcentre.saffron.web.api.TaxonomyUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

public class MongoDBHandler implements SaffronDataSource {

    final String url;
    final int port;
    String dbName;
    final String collectionName;
    MongoClient mongoClient;
    final MongoDatabase database;
    final MongoCollection runCollection;
    final MongoCollection topicsCollection;
    final MongoCollection topicsCorrespondenceCollection;
    final MongoCollection topicsExtractionCollection;
    final MongoCollection authorTopicsCollection;
    final MongoCollection topicsSimilarityCollection;
    final MongoCollection authorSimilarityCollection;
    final MongoCollection taxonomyCollection;
    final MongoCollection corpusCollection;

    private final Map<String, MongoDBHandler.SaffronDataImpl> data = new HashMap<>();

    public final String type = "mongodb";

    private static class SaffronDataImpl {

        private Taxonomy taxonomy;
        private List<AuthorAuthor> authorSim;
        private List<TopicTopic> topicSim;
        private List<AuthorTopic> authorTopics;
        private List<DocumentTopic> docTopics;
        private HashMap<String, Topic> topics;
        private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
        private HashMap<String, List<TopicTopic>> topicByTopic1, topicByTopic2;
        private HashMap<String, List<DocumentTopic>> docByTopic, topicByDoc;
        private HashMap<String, List<AuthorTopic>> authorByTopic, topicByAuthor;
        private List<String> topicsSorted;
        private HashMap<String, org.insightcentre.nlp.saffron.data.Document> corpus;
        private HashMap<String, List<org.insightcentre.nlp.saffron.data.Document>> corpusByAuthor;
        private HashMap<String, Author> authors;
        private HashMap<String, IntList> taxoMap;
        private DocumentSearcher searcher;
        private final String id;

        public SaffronDataImpl(String id) {
            this.id = id;
        }

        public Taxonomy getTaxonomy() {
            return taxonomy;
        }

        public void setTaxonomy(Taxonomy taxonomy) {
            this.taxonomy = taxonomy;
            this.taxoMap = getTaxoLocations(taxonomy);
        }

        public List<AuthorAuthor> getAuthorSim() {
            return authorSim;
        }

        public void setAuthorSim(List<AuthorAuthor> authorSim) {
            authorByAuthor1 = new HashMap<>();
            authorByAuthor2 = new HashMap<>();
            for (AuthorAuthor aa : authorSim) {
                if (!authorByAuthor1.containsKey(aa.author1_id)) {
                    authorByAuthor1.put(aa.author1_id, new ArrayList<AuthorAuthor>());
                }
                authorByAuthor1.get(aa.author1_id).add(aa);
                if (!authorByAuthor2.containsKey(aa.author2_id)) {
                    authorByAuthor2.put(aa.author2_id, new ArrayList<AuthorAuthor>());
                }
                authorByAuthor2.get(aa.author2_id).add(aa);
            }

            this.authorSim = authorSim;
        }

        public List<AuthorAuthor> getAuthorSimByAuthor1(String author1) {
            List<AuthorAuthor> aas = authorByAuthor1.get(author1);
            return aas == null ? Collections.EMPTY_LIST : aas;
        }

        public List<AuthorAuthor> getAuthorSimByAuthor2(String author2) {
            List<AuthorAuthor> aas = authorByAuthor2.get(author2);
            return aas == null ? Collections.EMPTY_LIST : aas;
        }

        public List<Author> authorAuthorToAuthor1(List<AuthorAuthor> aas) {
            List<Author> as = new ArrayList<>();
            for (AuthorAuthor aa : aas) {
                Author a = getAuthor(aa.author1_id);
                if (a != null) {
                    as.add(a);
                }
            }
            return as;
        }

        public List<Author> authorAuthorToAuthor2(List<AuthorAuthor> aas) {
            List<Author> as = new ArrayList<>();
            for (AuthorAuthor aa : aas) {
                Author a = getAuthor(aa.author2_id);
                if (a != null) {
                    as.add(a);
                }
            }
            return as;
        }

        public List<AuthorTopic> getAuthorTopics() {
            return authorTopics;
        }

        public void setAuthorTopics(Collection<AuthorTopic> authorTopics) {
            authorByTopic = new HashMap<>();
            topicByAuthor = new HashMap<>();
            for (AuthorTopic at : authorTopics) {
                if (!authorByTopic.containsKey(at.topic_id)) {
                    authorByTopic.put(at.topic_id, new ArrayList<AuthorTopic>());
                }
                authorByTopic.get(at.topic_id).add(at);
                if (!topicByAuthor.containsKey(at.author_id)) {
                    topicByAuthor.put(at.author_id, new ArrayList<AuthorTopic>());
                }
                topicByAuthor.get(at.author_id).add(at);
            }
            this.authorTopics = new ArrayList<>(authorTopics);
        }

        public List<AuthorTopic> getAuthorByTopic(String topic) {
            if (authorByTopic == null){
                authorByTopic = new HashMap<>();
            }
            List<AuthorTopic> ats = authorByTopic.get(topic);
            return ats == null ? Collections.EMPTY_LIST : ats;
        }

        public List<Author> authorTopicsToAuthors(List<AuthorTopic> ats) {
            List<Author> authors = new ArrayList<>();
            for (AuthorTopic at : ats) {
                Author a = getAuthor(at.author_id);
                if (a != null) {
                    authors.add(a);
                }
            }
            return authors;
        }

        public List<AuthorTopic> getTopicByAuthor(String author) {
            List<AuthorTopic> ats = topicByAuthor.get(author);
            return ats == null ? Collections.EMPTY_LIST : ats;
        }

        public List<DocumentTopic> getDocTopics() {
            return docTopics;
        }

        public void setDocTopics(List<DocumentTopic> docTopics) {
            docByTopic = new HashMap<>();
            topicByDoc = new HashMap<>();
            for (DocumentTopic dt : docTopics) {
                if (!docByTopic.containsKey(dt.topic_string)) {
                    docByTopic.put(dt.topic_string, new ArrayList<DocumentTopic>());
                }
                docByTopic.get(dt.topic_string).add(dt);
                if (!topicByDoc.containsKey(dt.document_id)) {
                    topicByDoc.put(dt.document_id, new ArrayList<DocumentTopic>());
                }
                topicByDoc.get(dt.document_id).add(dt);
            }
            this.docTopics = docTopics;
        }

        public List<org.insightcentre.nlp.saffron.data.Document> getDocByTopic(String topic) {
            final List<DocumentTopic> dts = docByTopic.get(topic);
            if (dts == null) {
                return Collections.EMPTY_LIST;
            } else {
                final List<org.insightcentre.nlp.saffron.data.Document> docs = new ArrayList<>();
                for (DocumentTopic dt : dts) {
                    org.insightcentre.nlp.saffron.data.Document d = corpus.get(dt.document_id);
                    if (d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        }

        public List<DocumentTopic> getTopicByDoc(String doc) {
            List<DocumentTopic> dts = topicByDoc.get(doc);
            if (dts == null) {
                return Collections.EMPTY_LIST;
            } else {
                return dts;
            }
        }

        public Collection<String> getTopTopics(int from, int to) {
            if (from < topicsSorted.size() && to <= topicsSorted.size()) {
                return topicsSorted.subList(from, to);
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public Topic getTopic(String topic) {
            return topics.get(topic);
        }

        public Collection<Topic> getTopics() {
            return topics == null ? Collections.EMPTY_LIST : topics.values();
        }

        public void setTopics(Collection<Topic> _topics) {
            this.topics = new HashMap<>();
            this.topicsSorted = new ArrayList<>();
            for (Topic t : _topics) {
                this.topics.put(t.topicString, t);
                this.topicsSorted.add(t.topicString);
            }
            this.topicsSorted.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (topics.containsKey(o1) && topics.containsKey(o2)) {
                        double wt1 = topics.get(o1).score;
                        double wt2 = topics.get(o2).score;
                        if (wt1 > wt2) {
                            return -1;
                        } else if (wt2 > wt1) {
                            return +1;
                        }
                    }
                    return o1.compareTo(o2);
                }
            });
        }

        public List<TopicTopic> getTopicSim() {
            return topicSim;
        }

        public void setTopicSim(List<TopicTopic> topicSim) {
            topicByTopic1 = new HashMap<>();
            topicByTopic2 = new HashMap<>();
            for (TopicTopic tt : topicSim) {
                if (!topicByTopic1.containsKey(tt.topic1)) {
                    topicByTopic1.put(tt.topic1, new ArrayList<TopicTopic>());
                }
                topicByTopic1.get(tt.topic1).add(tt);
                if (!topicByTopic2.containsKey(tt.topic2)) {
                    topicByTopic2.put(tt.topic2, new ArrayList<TopicTopic>());
                }
                topicByTopic2.get(tt.topic2).add(tt);
            }
            this.topicSim = topicSim;
        }

        public List<TopicTopic> getTopicByTopic1(String topic1, List<String> _ignore) {
            Set<String> ignore = _ignore == null ? new HashSet<>() : new HashSet<>(_ignore);
            List<TopicTopic> tt = topicByTopic1.get(topic1);
            if (tt != null) {
                Iterator<TopicTopic> itt = tt.iterator();
                while (itt.hasNext()) {
                    if (ignore.contains(itt.next().topic2)) {
                        itt.remove();
                    }
                }
                return tt;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public List<TopicTopic> getTopicByTopic2(String topic2) {
            List<TopicTopic> tt = topicByTopic2.get(topic2);
            return tt == null ? Collections.EMPTY_LIST : tt;
        }

        /**
         * Is the Saffron data available. If this is false the getters of this
         * class may return null;
         *
         * @return true if the code is loaded
         */
        public boolean isLoaded() {
            return taxonomy != null && authorSim != null && topicSim != null
                    && authorTopics != null && docTopics != null && topics != null
                    && corpus != null;
        }

        public void setCorpus(JSONObject corpus) {
            this.corpus = new HashMap<>();
            this.corpusByAuthor = new HashMap<>();
            this.authors = new HashMap<>();

            JSONArray documentArray = (JSONArray) corpus.get("documents");
            ArrayList<org.insightcentre.nlp.saffron.data.Document> docData =
                    new ArrayList<org.insightcentre.nlp.saffron.data.Document>();
            if (documentArray != null) {
                for (int i=0;i<documentArray.length();i++){

                    JSONObject obj = documentArray.getJSONObject(i);
                    String contents = "";
                    if (obj.has("contents"))
                        contents = obj.getString("contents");
                    SaffronPath path = new SaffronPath();
                    path.setPath(obj.getString("id"));
                    List<Author> authors = new ArrayList<>();
                    org.insightcentre.nlp.saffron.data.Document doc = new org.insightcentre.nlp.saffron.data.Document(
                            path, obj.getString("id"), null, obj.getString("name"), obj.getString("mime_type"),
                            Collections.EMPTY_LIST, Collections.EMPTY_MAP, contents);

                    docData.add(doc);
                }
            }
            for (org.insightcentre.nlp.saffron.data.Document d : docData) {
                this.corpus.put(d.id, d);
                for (Author a : d.getAuthors()) {
                    if (!corpusByAuthor.containsKey(a.id)) {
                        corpusByAuthor.put(a.id, new ArrayList<org.insightcentre.nlp.saffron.data.Document>());
                    }
                    corpusByAuthor.get(a.id).add(d);
                    if (!authors.containsKey(a.id)) {
                        authors.put(a.id, a);
                    }
                }
            }
            //this.searcher = corpus;
        }

        public DocumentSearcher getSearcher() {
            return searcher;
        }

        public List<org.insightcentre.nlp.saffron.data.Document> getDocsByAuthor(String authorId) {
            List<org.insightcentre.nlp.saffron.data.Document> docs = corpusByAuthor.get(authorId);
            return docs == null ? Collections.EMPTY_LIST : docs;
        }

        public Author getAuthor(String authorId) {
            return authors.get(authorId);
        }

        public Collection<Author> getAuthors() {
            return authors.values();
        }

        public org.insightcentre.nlp.saffron.data.Document getDoc(String docId) {
            return corpus.get(docId);
        }

        public Collection<org.insightcentre.nlp.saffron.data.Document> getDocuments() {
            return corpus.values();
        }

        private HashMap<String, IntList> getTaxoLocations(Taxonomy t) {
            IntList il = new IntArrayList();
            HashMap<String, IntList> map = new HashMap<>();
            _getTaxoLocations(t, il, map);
            return map;
        }

        private void _getTaxoLocations(Taxonomy t, IntList il, HashMap<String, IntList> map) {
            map.put(t.root, il);
            for (int i = 0; i < t.children.size(); i++) {
                IntList il2 = new IntArrayList(il);
                il2.add(i);
                _getTaxoLocations(t.children.get(i), il2, map);
            }
        }

        private Taxonomy taxoNavigate(Taxonomy t, IntList il) {
            for (int i : il) {
                t = t.children.get(i);
            }
            return t;
        }

        public List<String> getTaxoParents(String topic_string) {
            IntList il = taxoMap.get(topic_string);
            if (il != null) {
                Taxonomy t = taxonomy;
                List<String> route = new ArrayList<>();
                for (int i : il) {
                    route.add(t.root);
                    t = t.children.get(i);
                }
                return route;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public Taxonomy getTaxoDescendent(String topic_string) {

            Taxonomy t = taxonomy;
            return taxonomy.descendent(topic_string);
        }

        public List<String> getTaxoChildren(String topic_string) {
            IntList il = taxoMap.get(topic_string);
            if (il != null) {
                Taxonomy t = taxoNavigate(taxonomy, il);
                List<String> children = new ArrayList<>();
                for (Taxonomy t2 : t.children) {
                    children.add(t2.root);
                }
                return children;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public List<TopicAndScore> getTaxoChildrenScored(String topic_string) {
            IntList il = taxoMap.get(topic_string);
            if (il != null) {
                Taxonomy t = taxoNavigate(taxonomy, il);
                List<TopicAndScore> children = new ArrayList<>();
                for (Taxonomy t2 : t.children) {
                    children.add(new TopicAndScore(t2.root, t2.linkScore));
                }
                return children;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        private void updateTopicName(String topic, String newTopic, Status status) {
            Topic t = topics.get(topic);
            if (t != null) {
                for (AuthorTopic at : authorTopics) {
                    if (at.topic_id.equals(topic)) {
                        at.topic_id = newTopic;
                    }
                }
                for (DocumentTopic dt : docTopics) {
                    if (dt.topic_string.equals(topic)) {
                        dt.topic_string = newTopic;
                    }
                }
                for (TopicTopic tt : topicSim) {
                    if (tt.topic1.equals(topic)) {
                        tt.topic1 = newTopic;
                    }
                    if (tt.topic2.equals(topic)) {
                        tt.topic2 = newTopic;
                    }
                }
                updateTopicNameInTaxonomy(taxonomy, topic, newTopic);
                t.topicString = newTopic;
                t.status = status;
            }
        }

        private void updateTopicNameInTaxonomy(Taxonomy taxo, String topic, String newTopic) {
            if(taxo.root.equals(topic)) {
                taxo.root = newTopic;
            } else {
                for(Taxonomy child : taxo.getChildren()) {
                    updateTopicNameInTaxonomy(child, topic, newTopic);
                }
            }
        }
    }

    public MongoDBHandler(String url, int port, String dbName, String collectionName) {
        this.url = url;
        this.port = port;
        this.dbName = dbName;
        this.collectionName = collectionName;
        MongoClientOptions options = MongoClientOptions.builder().cursorFinalizerEnabled(false).build();

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

    /**
     * Load the Saffron data from mongo
     *
     * @param runId The runId for the saffron instance
     * @return An initializes object
     * @throws IOException
     */
    public void fromMongo(String runId) throws IOException {

        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();
        final MongoDBHandler.SaffronDataImpl saffron = new MongoDBHandler.SaffronDataImpl(runId);

        Taxonomy docs = this.getTaxonomy(runId);
        saffron.setTaxonomy(docs);

        JSONArray authorSimDocsArray = new JSONArray();
        Iterable<org.bson.Document> authorSimDocs = this.getAuthorSimilarity(runId);
        for (org.bson.Document doc : authorSimDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            authorSimDocsArray.put(jsonObj);
        }
        saffron.setAuthorSim((List<AuthorAuthor>) mapper.readValue(authorSimDocsArray.toString(),
                tf.constructCollectionType(List.class, AuthorAuthor.class)));

        Iterable<org.bson.Document> topicSimDocs = this.getTopicsSimilarity(runId);
        JSONArray topicSimDocsArray = new JSONArray();
        for (org.bson.Document doc : topicSimDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            topicSimDocsArray.put(jsonObj);
        }
        saffron.setTopicSim((List<TopicTopic>) mapper.readValue(topicSimDocsArray.toString(),
                tf.constructCollectionType(List.class, TopicTopic.class)));

        Iterable<org.bson.Document> authorTopicsDocs = this.getAuthorTopics(runId);
        JSONArray authorTopicsDocsArray = new JSONArray();
        for (org.bson.Document doc : authorTopicsDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            authorTopicsDocsArray.put(jsonObj);
        }
        saffron.setAuthorTopics((List<AuthorTopic>) mapper.readValue(authorTopicsDocsArray.toString(),
                tf.constructCollectionType(List.class, AuthorTopic.class)));


        Iterable<org.bson.Document> docTopicsDocs = this.getDocumentTopicCorrespondence(runId);
        JSONArray docTopicsArray = new JSONArray();
        for (org.bson.Document doc : docTopicsDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());

            docTopicsArray.put(jsonObj);
        }
        saffron.setDocTopics((List<DocumentTopic>) mapper.readValue(docTopicsArray.toString(),
                tf.constructCollectionType(List.class, DocumentTopic.class)));

        Iterable<org.bson.Document> topicsDocs = this.getTopics(runId);
        JSONArray jsonTopicsArray = new JSONArray();
        for (org.bson.Document doc : topicsDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            jsonTopicsArray.put(jsonObj);
        }
        saffron.setTopics((List<Topic>) mapper.readValue(jsonTopicsArray.toString(),
                tf.constructCollectionType(List.class, Topic.class)));

        Iterable<org.bson.Document> corpus = this.getCorpus(runId);
        for (org.bson.Document doc : corpus) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            saffron.setCorpus(jsonObj);
        }

        this.data.put(runId, saffron);
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbName() {
        return this.dbName;
    }

    public boolean addRun(String id, Date date, Configuration config) {
        final MongoDBHandler.SaffronDataImpl saffron = new MongoDBHandler.SaffronDataImpl(id);
        this.data.put(id, saffron);
        Gson gson = new Gson();
        String json = gson.toJson(config);
        Document document = new Document();
        document.put("id", id);
        document.put("run_date", date);
        document.put("config", json);
        this.runCollection.insertOne(document);
        try {
            //this.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public FindIterable<Document>  getAllRuns() {
        FindIterable<Document> docs = MongoUtils.getDocs(this);
        return docs;
    }



    public void deleteRun(String name) {
        Document document = new Document();
        document.put("run", name);
        MongoUtils.deleteRunFromMongo(name, this);
    }



    @Override
    public String getRun(String runId) {
        JSONObject jsonObj = new JSONObject();

        try {
            //MongoDBHandler mongo = new MongoDBHandler("localhost", 27017, "saffron", "saffron_runs");

            Iterable<org.bson.Document> docs = getRunFromMongo(runId, runCollection);
            for (org.bson.Document doc : docs) {
                jsonObj = new JSONObject(doc.toJson());

            }
            //this.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObj.toString();
    }

    public static FindIterable getRunFromMongo(String runId, MongoCollection runCollection) {
        return runCollection.find(and(eq("id", runId)));
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
            document.put("topic_string", name.topic_string);
            document.put("acronym", name.acronym);
            document.put("occurences", name.occurrences);
            document.put("pattern", name.pattern);
            document.put("tfidf", name.tfidf);
            document.put("document_id", name.document_id);
            topicsCorrespondenceCollection.insertOne(document);
        });
        return true;
    }



    public FindIterable<Document> getDocumentTopicCorrespondence(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsCorrespondenceCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getDocumentTopicCorrespondenceForTopic(String runId, String topicId) {

        return topicsCorrespondenceCollection.find(and(eq("run", runId), eq("topic", topicId)));
    }


    public FindIterable<Document> getDocumentTopicCorrespondenceForDocument(String runId, String docId) {
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
            document.put("topic_string", name.topicString);
            document.put("mvList", name.mvList);
            document.put("dbpedia_url", name.dbpedia_url);
            document.put("status", "none");
            topicsCollection.insertOne(document);
        });



        return true;
    }

    public FindIterable<Document> getTopics(String runId) {
        FindIterable<Document> docs = MongoUtils.getTopicsFromMongo(runId, this);
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




    public boolean addAuthorTopics(String id, Date date, Collection<AuthorTopic> topics) {
        Document document = new Document();
        int[] idx = { 0 };
        topics.forEach(name -> {
            document.put("_id", id + "_" + name.topic_id + "_" + idx[0]++);
            document.put("run", id);
            document.put("run_date", date);
            document.put("author_topic", name.topic_id);
            document.put("matches", name.matches);
            document.put("occurences", name.occurrences);
            document.put("score", name.score);
            document.put("topic_id", name.topic_id);
            document.put("tfirf", name.tfirf);
            document.put("paper_count", name.paper_count);
            document.put("researcher_score", name.researcher_score);
            authorTopicsCollection.insertOne(document);
        });
        return true;
    }

    public FindIterable<Document>  getAuthorTopics(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.authorTopicsCollection.find(and(eq("run", runId)));

    }

    public FindIterable<Document>  getAuthorTopicsForTopic(String runId, String topic) {
        Document document = new Document();
        document.put("run", runId);
        return authorTopicsCollection.find(and(eq("run", runId), eq("author_topic", topic)));

    }

    public boolean addTopicsSimilarity(String id, Date date, List<TopicTopic> topicSimilarity) {
        Document document = new Document();
        int[] idx = { 0 };
        for (TopicTopic topic : topicSimilarity) {
            document.put("_id", id + "_" + topic.getTopic1() + "_" + topic.getTopic2() + "_" + idx[0]++);
            document.put("run", id);
            document.put("run_date", date);
            document.put("topic1_id", topic.getTopic1());
            document.put("topic2_id", topic.getTopic2());
            document.put("similarity", topic.getSimilarity());
            topicsSimilarityCollection.insertOne(document);
        }
        return true;
    }

    public FindIterable<Document> getTopicsSimilarity(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsSimilarityCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getTopicsSimilarityBetweenTopics(String runId, String topic1, String topic2) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsSimilarityCollection.find(and(eq("run", runId), eq("topic1_id", topic1), eq("topic2", topic2)));
    }

    public FindIterable<Document> getTopicsSimilarityForTopic(String runId, String topic) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsSimilarityCollection.find(and(eq("run", runId), eq("topic1_id", topic)));
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
        return this.authorSimilarityCollection.find(and(eq("run", runId)));
    }


    public FindIterable<Document> getAuthorSimilarityForTopic(String runId, String topic1, String topic2) {
        Document document = new Document();
        document.put("run", runId);
        return this.authorSimilarityCollection.find(and(eq("run", runId), eq("topic1_id", topic1), eq("topic2_id", topic2)));
    }

    public boolean addTaxonomy(String id, Date date, Taxonomy graph) {

        ObjectMapper mapper = new ObjectMapper();


        try{
            Document doc = Document.parse( mapper.writeValueAsString(graph) );
            doc.append("id", id);
            doc.append("date", date);
            this.taxonomyCollection.insertOne(doc);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
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
        Taxonomy graph = new Taxonomy("", 0, 0, "", "", new ArrayList<>(), Status.none);
        FindIterable<Document> docs = MongoUtils.getTaxonomyFromMongo(runId, this);
        graph = TaxonomyUtils.getTaxonomyFromDocs(docs, graph);
        return graph;
    }




    @Override
    public List<DocumentTopic> getDocTopics(String runId) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);

        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocTopics();
    }

    @Override
    public List<String> getTaxoParents(String runId, String topic_string) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoParents(topic_string);
    }

    @Override
    public List<TopicAndScore> getTaxoChildrenScored(String runId, String topic_string) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildrenScored(topic_string);
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor1(String runId, String author1) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorSimByAuthor1(author1);
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor2(String runId, String author1) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorSimByAuthor2(author1);
    }

    @Override
    public List<Author> authorAuthorToAuthor1(String runId, List<AuthorAuthor> aas) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorAuthorToAuthor1(aas);
    }

    @Override
    public List<Author> authorAuthorToAuthor2(String runId, List<AuthorAuthor> aas) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorAuthorToAuthor2(aas);
    }

    @Override
    public List<String> getTaxoChildren(String runId, String topic_string) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildren(topic_string);
    }

    @Override
    public List<TopicTopic> getTopicByTopic1(String runId, String topic1, List<String> _ignore) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByTopic1(topic1, _ignore);
    }

    @Override
    public List<TopicTopic> getTopicByTopic2(String runId, String topic2) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByTopic2(topic2);
    }

    @Override
    public List<AuthorTopic> getTopicByAuthor(String runId, String author) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByAuthor(author);
    }

    @Override
    public List<AuthorTopic> getAuthorByTopic(String runId, String topic) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorByTopic(topic);
    }

    @Override
    public List<Author> authorTopicsToAuthors(String runId, List<AuthorTopic> ats) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorTopicsToAuthors(ats);
    }

    @Override
    public List<DocumentTopic> getTopicByDoc(String runId, String doc) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByDoc(doc);
    }

    @Override
    public List<org.insightcentre.nlp.saffron.data.Document> getDocByTopic(String runId, String topic) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocByTopic(topic);
    }

    @Override
    public Topic getTopic(String runId, String topic) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopic(topic);
    }

    @Override
    public List<org.insightcentre.nlp.saffron.data.Document> getDocsByAuthor(String runId, String authorId) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocsByAuthor(authorId);
    }

    @Override
    public Collection<String> getTopTopics(String runId, int from, int to) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopTopics(from, to);
    }

    @Override
    public Author getAuthor(String runId, String authorId) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthor(authorId);
    }

    @Override
    public org.insightcentre.nlp.saffron.data.Document getDoc(String runId, String docId) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDoc(docId);
    }

    @Override
    public DocumentSearcher getSearcher(String runId) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getSearcher();
    }

    @Override
    public void setDocTopics(String runId, List<DocumentTopic> docTopics) {
        this.addDocumentTopicCorrespondence(runId, new Date(), docTopics);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setDocTopics(docTopics);
    }

    @Override
    public void setCorpus(String runId, DocumentSearcher corpus) {}

    @Override
    public void setCorpus(String runId, Corpus corpus) {

        this.addCorpus(runId, new Date(), corpus);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        Iterable<org.bson.Document> corpusJson = this.getCorpus(runId);
        for (org.bson.Document doc : corpusJson) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            saffron.setCorpus(jsonObj);
        }
    }

    @Override
    public void setTopics(String runId, List<Topic> _topics) {
        this.addTopics(runId, new Date(), _topics);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTopics(_topics);

    }

    @Override
    public void setAuthorTopics(String runId, Collection<AuthorTopic> authorTopics) {
       this.addAuthorTopics(runId, new Date(), authorTopics);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setAuthorTopics(authorTopics);
    }

    @Override
    public void setTopicSim(String runId, List<TopicTopic> topicSim) {
        this.addTopicsSimilarity(runId, new Date(), topicSim);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTopicSim(topicSim);
    }

    @Override
    public void setAuthorSim(String runId, List<AuthorAuthor> authorSim) {
        this.addAuthorSimilarity(runId, new Date(), authorSim);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setAuthorSim(authorSim);
    }

    @Override
    public void setTaxonomy(String runId, Taxonomy taxonomy) {
        this.addTaxonomy(runId, new Date(), taxonomy);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTaxonomy(taxonomy);
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
    public boolean containsKey(String id) throws IOException {
            String run = this.getRun(id);
            if (!run.matches("\\{}")) {
                //this.fromMongo(id);
                return true;
            }
        return false;
    }

    @Override
    public boolean isLoaded(String id) {
        String run = this.getRun(id);
        if (!run.matches("\\{}")) {
            return true;
        }
        else {
            throw new NoSuchElementException("Saffron run does not exist");
        }

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
                this.corpusCollection.findOneAndDelete(doc);

            this.corpusCollection.insertOne(doc);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
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
        return this.corpusCollection.find(and(eq("id", saffronDatasetName)));
    }
}
