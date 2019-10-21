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
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.saffron.web.SaffronDataSource;
import org.insightcentre.saffron.web.api.TaxonomyUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        private List<DocumentTerm> docTopics;
        private HashMap<String, Term> terms;
        private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
        private HashMap<String, List<TopicTopic>> topicByTopic1, topicByTopic2;
        private HashMap<String, List<DocumentTerm>> docByTopic, topicByDoc;
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

        public List<AuthorTopic> getAuthorByTopic(String term) {
            if (authorByTopic == null){
                authorByTopic = new HashMap<>();
            }
            List<AuthorTopic> ats = authorByTopic.get(term);
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

        public List<DocumentTerm> getDocTopics() {
            return docTopics;
        }

        public void setDocTopics(List<DocumentTerm> docTopics) {
            docByTopic = new HashMap<>();
            topicByDoc = new HashMap<>();
            for (DocumentTerm dt : docTopics) {
                if (!docByTopic.containsKey(dt.getTermString())) {
                    docByTopic.put(dt.getTermString(), new ArrayList<DocumentTerm>());
                }
                docByTopic.get(dt.getTermString()).add(dt);
                if (!topicByDoc.containsKey(dt.getDocumentId())) {
                    topicByDoc.put(dt.getDocumentId(), new ArrayList<DocumentTerm>());
                }
                topicByDoc.get(dt.getDocumentId()).add(dt);
            }
            this.docTopics = docTopics;
        }

        public List<org.insightcentre.nlp.saffron.data.Document> getDocByTopic(String term) {
            final List<DocumentTerm> dts = docByTopic.get(term);
            if (dts == null) {
                return Collections.EMPTY_LIST;
            } else {
                final List<org.insightcentre.nlp.saffron.data.Document> docs = new ArrayList<>();
                for (DocumentTerm dt : dts) {
                    org.insightcentre.nlp.saffron.data.Document d = corpus.get(dt.getDocumentId());
                    if (d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        }

        public List<DocumentTerm> getTopicByDoc(String doc) {
            List<DocumentTerm> dts = topicByDoc.get(doc);
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

        public Term getTopic(String term) {
            return terms.get(term);
        }

        public Collection<Term> getTopics() {
            return terms == null ? Collections.EMPTY_LIST : terms.values();
        }

        public void setTopics(Collection<Term> _terms) {
            this.terms = new HashMap<>();
            this.topicsSorted = new ArrayList<>();
            for (Term t : _terms) {
                this.terms.put(t.getString(), t);
                this.topicsSorted.add(t.getString());
            }
            this.topicsSorted.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (terms.containsKey(o1) && terms.containsKey(o2)) {
                        double wt1 = terms.get(o1).getScore();
                        double wt2 = terms.get(o2).getScore();
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

        public void setTopicSim(List<TopicTopic> termSim) {
            topicByTopic1 = new HashMap<>();
            topicByTopic2 = new HashMap<>();
            for (TopicTopic tt : termSim) {
                if (!topicByTopic1.containsKey(tt.topic1)) {
                    topicByTopic1.put(tt.topic1, new ArrayList<TopicTopic>());
                }
                topicByTopic1.get(tt.topic1).add(tt);
                if (!topicByTopic2.containsKey(tt.topic2)) {
                    topicByTopic2.put(tt.topic2, new ArrayList<TopicTopic>());
                }
                topicByTopic2.get(tt.topic2).add(tt);
            }
            this.topicSim = termSim;
        }

        public List<TopicTopic> getTopicByTopic1(String term1, List<String> _ignore) {
            Set<String> ignore = _ignore == null ? new HashSet<>() : new HashSet<>(_ignore);
            List<TopicTopic> tt = topicByTopic1.get(term1);
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

        public List<TopicTopic> getTopicByTopic2(String term2) {
            List<TopicTopic> tt = topicByTopic2.get(term2);
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
                    && authorTopics != null && docTopics != null && terms != null
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

        public void setSearcher(DocumentSearcher searcher) {
        	this.searcher = searcher;
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

        public List<String> getTaxoParents(String term_string) {
            IntList il = taxoMap.get(term_string);
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

        public Taxonomy getTaxoDescendent(String term_string) {

            Taxonomy t = taxonomy;
            return taxonomy.descendent(term_string);
        }

        public List<String> getTaxoChildren(String term_string) {
            IntList il = taxoMap.get(term_string);
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

        public List<TopicAndScore> getTaxoChildrenScored(String term_string) {
            IntList il = taxoMap.get(term_string);
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

        private void updateTopicName(String term, String newTerm, Status status) {
            Term t = terms.get(term);
            if (t != null) {
                for (AuthorTopic at : authorTopics) {
                    if (at.topic_id.equals(term)) {
                        at.topic_id = newTerm;
                    }
                }
                for (DocumentTerm dt : docTopics) {
                    if (dt.getTermString().equals(term)) {
                        dt.setTermString(newTerm);
                    }
                }
                for (TopicTopic tt : topicSim) {
                    if (tt.topic1.equals(term)) {
                        tt.topic1 = newTerm;
                    }
                    if (tt.topic2.equals(term)) {
                        tt.topic2 = newTerm;
                    }
                }
                updateTopicNameInTaxonomy(taxonomy, term, newTerm);
                t.setString(newTerm);
                t.setStatus(status);
            }
        }

        private void updateTopicNameInTaxonomy(Taxonomy taxo, String term, String newTerm) {
            if(taxo.root.equals(term)) {
                taxo.root = newTerm;
            } else {
                for(Taxonomy child : taxo.getChildren()) {
                    updateTopicNameInTaxonomy(child, term, newTerm);
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
        this.topicsCollection = database.getCollection(collectionName + "_terms");
        this.topicsCorrespondenceCollection = database.getCollection(collectionName + "_terms_correspondence");
        this.topicsExtractionCollection = database.getCollection(collectionName + "_terms_extraction");
        this.authorTopicsCollection = database.getCollection(collectionName + "_author_terms");
        this.topicsSimilarityCollection = database.getCollection(collectionName + "_terms_similarity");
        this.authorSimilarityCollection = database.getCollection(collectionName + "_author_similarity");
        this.taxonomyCollection = database.getCollection(collectionName + "_taxonomy");
        this.corpusCollection = database.getCollection(collectionName + "_corpus");

        this.initialiseInMemoryDatabase();
    }

    private void initialiseInMemoryDatabase() {
        try {
    	    FindIterable<org.bson.Document> runs = this.getAllRuns();

            for (org.bson.Document doc : runs) {
                JSONObject configObj = new JSONObject(doc);
                configObj.get("id").toString();
                this.fromMongo(configObj.get("id").toString());
            }
        } catch (JSONException e) {
            throw new RuntimeException("An error has ocurring while loading database into memory", e);
        } catch (IOException e) {
            throw new RuntimeException("An error has ocurring while loading database into memory", e);
        }
	}
    
    /**
     * Load Saffron data from disk into MongoDB
     *
     * @param directory The directory containing the JSON files
     * @return nothing
     * @throws IOException
     */
    public void importFromDirectory(File directory, String name) throws IOException {
    	
    	String runId = directory.getName();
    	
    	File configFile = new File(directory, "config.json");
    	if (!configFile.exists()) {
            throw new FileNotFoundException("Could not find config.json");
        }
    	
        File taxonomyFile = new File(directory, "taxonomy.json");
        if (!taxonomyFile.exists()) {
            throw new FileNotFoundException("Could not find taxonomy.json");
        }
        
        File authorSimFile = new File(directory, "author-sim.json");
        if (!authorSimFile.exists()) {
            throw new FileNotFoundException("Could not find author-sim.json");
        }
        
        File topicSimFile = new File(directory, "topic-sim.json");
        if (!topicSimFile.exists()) {
            throw new FileNotFoundException("Could not find topic-sim.json");
        }
        
        File authorTopicFile = new File(directory, "author-topics.json");
        if (!authorTopicFile.exists()) {
            throw new FileNotFoundException("Could not find author-topics.json");
        }
        
        File docTopicsFile = new File(directory, "doc-topics.json");
        if (!docTopicsFile.exists()) {
            throw new FileNotFoundException("Could not find doc-topics.json");
        }
        
        File topicsFile = new File(directory, "topics.json");
        if (!topicsFile.exists()) {
            throw new FileNotFoundException("Could not find topics.json");
        }
        
        File indexFile = new File(directory, "index");
        if (!indexFile.exists()) {
            throw new FileNotFoundException("Could not find index");
        }
        
    	BufferedReader r = Files.newBufferedReader(Paths.get(configFile.getAbsolutePath()));
    	StringBuilder sb = new StringBuilder();
        Configuration newConfig = null;
        try {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
            newConfig = new ObjectMapper().readValue(sb.toString(), Configuration.class);
        } catch (Exception e) {
        	throw new RuntimeException("The configuration file is in the wrong format.", e);
        }        
    	this.addRun(runId, new Date(), newConfig);
    	
    	
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();
        
        this.setTaxonomy(runId, mapper.readValue(taxonomyFile, VirtualRootTaxonomy.class));
        this.setAuthorSim(runId, mapper.readValue(authorSimFile,
                tf.constructCollectionType(List.class, AuthorAuthor.class)));
        this.setTopicSim(runId,  mapper.readValue(topicSimFile,
                tf.constructCollectionType(List.class, TopicTopic.class)));
        this.setAuthorTopics(runId, mapper.readValue(authorTopicFile,
                tf.constructCollectionType(List.class, AuthorTopic.class)));
        this.setDocTopics(runId, mapper.readValue(docTopicsFile,
                tf.constructCollectionType(List.class, DocumentTerm.class)));
        this.setTopics(runId, mapper.readValue(topicsFile,
                tf.constructCollectionType(List.class, Term.class)));
        this.setCorpus(runId, DocumentSearcherFactory.load(indexFile));
        this.setIndex(runId, DocumentSearcherFactory.load(indexFile));
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
        saffron.setDocTopics((List<DocumentTerm>) mapper.readValue(docTopicsArray.toString(),
                tf.constructCollectionType(List.class, DocumentTerm.class)));

        Iterable<org.bson.Document> topicsDocs = this.getTopics(runId);
        JSONArray jsonTopicsArray = new JSONArray();
        for (org.bson.Document doc : topicsDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            jsonTopicsArray.put(jsonObj);
        }
        saffron.setTopics((List<Term>) mapper.readValue(jsonTopicsArray.toString(),
                tf.constructCollectionType(List.class, Term.class)));

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
        try {
            this.runCollection.insertOne(document);

            //this.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public FindIterable<Document>  getAllRuns() {
        try {
            FindIterable<Document> docs = MongoUtils.getDocs(this);
            return docs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    public FindIterable<Document> getTopicExtractionForTopic(String runId, String termId) {
        return topicsExtractionCollection.find(and(eq("run", runId), eq("termId", termId)));
    }

    //TODO: Create final variable with names of attributes in each collection
    public boolean addTopicExtraction(String id, Date date, Set<Term> res) {
        Document document = new Document();

        res.forEach(name -> {
            document.put("_id", id + "_" + name.getString());
            document.put("run", id);
            document.put("run_date", date);
            document.put("term", name.getString());
            document.put("score", name.getScore());
            document.put("dbpedia_url", name.getDbpediaUrl());
            document.put("mvList", name.getMorphologicalVariationList());
            document.put("occurrences", name.getOccurrences());
            document.put("matches", name.getMatches());
            topicsExtractionCollection.insertOne(document);
        });
        return true;
    }



    public boolean addDocumentTopicCorrespondence(String id, Date date, List<DocumentTerm> terms) {
        Document document = new Document();

        terms.forEach(name -> {
            document.put("_id", id + "_" + name.getTermString() + "_" + name.getDocumentId());
            document.put("run", id);
            document.put("run_date", date);
            document.put("term_string", name.getTermString());
            document.put("acronym", name.getAcronym());
            document.put("occurences", name.getOccurrences());
            document.put("pattern", name.getPattern());
            document.put("tfidf", name.getTfIdf());
            document.put("document_id", name.getDocumentId());
            topicsCorrespondenceCollection.insertOne(document);
        });
        return true;
    }



    public FindIterable<Document> getDocumentTopicCorrespondence(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsCorrespondenceCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getDocumentTopicCorrespondenceForTopic(String runId, String termId) {

        return topicsCorrespondenceCollection.find(and(eq("run", runId), eq("term", termId)));
    }


    public FindIterable<Document> getDocumentTopicCorrespondenceForDocument(String runId, String docId) {
        return topicsCorrespondenceCollection.find(and(eq("run", runId), eq("document_id", docId)));
    }

    public boolean addTopics(String id, Date date, List<Term> terms) {
        Document document = new Document();

        terms.forEach(name -> {
            document.put("_id", id + "_" + name.getString());
            document.put("run", id);
            document.put("run_date", date);
            document.put("term", name.getString());
            document.put("matches", name.getMatches());
            document.put("occurences", name.getOccurrences());
            document.put("score", name.getScore());
            document.put("term_string", name.getString());
            document.put("mvList", name.getMorphologicalVariationList());
            document.put("dbpedia_url", name.getDbpediaUrl());
            document.put("status", name.getStatus().toString());
            topicsCollection.insertOne(document);
        });



        return true;
    }

    public FindIterable<Document> getTopics(String runId) {
        FindIterable<Document> docs = MongoUtils.getTopicsFromMongo(runId, this);
        return docs;
    }


    public void deleteTopic(String runId, String term) {

        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("run", runId);
        updateFields.append("term", term);
        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);
        topicsCollection.findOneAndDelete(and(eq("run", runId), (eq("term", term))));
    }




    public boolean addAuthorTopics(String id, Date date, Collection<AuthorTopic> terms) {
        Document document = new Document();
        int[] idx = { 0 };
        terms.forEach(name -> {
            document.put("_id", id + "_" + name.topic_id + "_" + idx[0]++);
            document.put("run", id);
            document.put("run_date", date);
            document.put("author_topic", name.topic_id);
            document.put("matches", name.matches);
            document.put("occurences", name.occurrences);
            document.put("score", name.score);
            document.put("term_id", name.topic_id);
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

    public FindIterable<Document>  getAuthorTopicsForTopic(String runId, String term) {
        Document document = new Document();
        document.put("run", runId);
        return authorTopicsCollection.find(and(eq("run", runId), eq("author_term", term)));

    }

    public boolean addTopicsSimilarity(String id, Date date, List<TopicTopic> termSimilarity) {
        Document document = new Document();
        int[] idx = { 0 };
        for (TopicTopic term : termSimilarity) {
            document.put("_id", id + "_" + term.getTopic1() + "_" + term.getTopic2() + "_" + idx[0]++);
            document.put("run", id);
            document.put("run_date", date);
            document.put("term1_id", term.getTopic1());
            document.put("term2_id", term.getTopic2());
            document.put("similarity", term.getSimilarity());
            topicsSimilarityCollection.insertOne(document);
        }
        return true;
    }

    public FindIterable<Document> getTopicsSimilarity(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsSimilarityCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getTopicsSimilarityBetweenTopics(String runId, String term1, String term2) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsSimilarityCollection.find(and(eq("run", runId), eq("term1_id", term1), eq("term2", term2)));
    }

    public FindIterable<Document> getTopicsSimilarityForTopic(String runId, String term) {
        Document document = new Document();
        document.put("run", runId);
        return this.topicsSimilarityCollection.find(and(eq("run", runId), eq("term1_id", term)));
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


    public FindIterable<Document> getAuthorSimilarityForTopic(String runId, String term1, String term2) {
        Document document = new Document();
        document.put("run", runId);
        return this.authorSimilarityCollection.find(and(eq("run", runId), eq("term1_id", term1), eq("term2_id", term2)));
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

    public boolean updateTaxonomy(String id, Taxonomy graph) {
        Document doc = new Document();
        doc.append("id", id);
        try {
            taxonomyCollection.findOneAndDelete(doc);
            this.addTaxonomy(id, new Date(), graph);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Failed to reject the term from the taxonomy " + id);
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
    public List<DocumentTerm> getDocTopics(String runId) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);

        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocTopics();
    }

    @Override
    public List<String> getTaxoParents(String runId, String term_string) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoParents(term_string);
    }

    @Override
    public List<TopicAndScore> getTaxoChildrenScored(String runId, String term_string) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildrenScored(term_string);
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
    public List<String> getTaxoChildren(String runId, String term_string) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildren(term_string);
    }

    @Override
    public List<TopicTopic> getTopicByTopic1(String runId, String term1, List<String> _ignore) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByTopic1(term1, _ignore);
    }

    @Override
    public List<TopicTopic> getTopicByTopic2(String runId, String term2) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByTopic2(term2);
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
    public List<AuthorTopic> getAuthorByTopic(String runId, String term) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorByTopic(term);
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
    public List<DocumentTerm> getTopicByDoc(String runId, String doc) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByDoc(doc);
    }

    @Override
    public List<org.insightcentre.nlp.saffron.data.Document> getDocByTopic(String runId, String term) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocByTopic(term);
    }

    @Override
    public Term getTopic(String runId, String term) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopic(term);
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
    public void setIndex(String runId, DocumentSearcher index) {
    	MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }        
        saffron.setSearcher(index);
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
    public void setDocTopics(String runId, List<DocumentTerm> docTopics) {
        this.addDocumentTopicCorrespondence(runId, new Date(), docTopics);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setDocTopics(docTopics);
    }

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
    public void setTopics(String runId, List<Term> _terms) {
        this.addTopics(runId, new Date(), _terms);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTopics(_terms);

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
    public void setTopicSim(String runId, List<TopicTopic> termSim) {
        this.addTopicsSimilarity(runId, new Date(), termSim);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTopicSim(termSim);
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


    public boolean updateTopic(String id, String term, String status) {

        try {
            Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));
            Bson update = set("status", status);


            FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
            findOptions.upsert(true);
            findOptions.returnDocument(ReturnDocument.AFTER);

            topicsCollection.findOneAndUpdate(condition, update, findOptions);
            return true;
        } catch (Exception e ) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }



    public boolean updateTopicSimilarity(String id, String term1, String term2, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term1", term1),
                Filters.eq("term2", term2));
        Bson update = set("status", status);



        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term1 + " from the taxonomy " + id);
            return false;
        }

    }

    public boolean updateAuthorTopicName(String id, String term, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("author_term", term));
        Bson update = combine(set("author_term", newTopic), set("termString", newTopic),
                set("originalTopic", term), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }


    public boolean updateDocumentTopicName(String id, String term, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));
        Bson update = combine(set("term", newTopic),
                set("originalTopic", term), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsCorrespondenceCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }

    public boolean updateTopicSimilarityName(String id, String term, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term1", term));
        Bson update = combine(set("term1", newTopic),
                set("originalTopic", term), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            topicsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }




    public boolean updateTopicName(String id, String term, String newTopic, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));
        Bson update = combine(set("term", newTopic), set("termString", newTopic), set("originalTopic", term));

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
    public Taxonomy getTaxoDescendent(String runId, String termString) {
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
    public Iterable<Term> getAllTopics(String datasetName) {
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
    public Iterable<DocumentTerm> getDocTopicByTopic(String name, String termId) {
        return null;
    }

    @Override
    public Iterable<TopicTopic> getAllTopicSimilarities(String name) {
        return null;
    }

    @Override
    public Iterable<TopicTopic> getTopicByTopics(String name, String term1, String term2) {
        return null;
    }


    public FindIterable<Document> searchTaxonomy(String id, String term) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));

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
