package org.insightcentre.saffron.web.mongodb;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.http.HttpServlet;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.saffron.web.Executor;
import org.insightcentre.saffron.web.Launcher;
import org.insightcentre.saffron.web.SaffronDataSource;
import org.insightcentre.saffron.web.api.TaxonomyUtils;
import org.insightcentre.saffron.web.exception.ConceptNotFoundException;
import org.insightcentre.saffron.web.exception.TermNotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class MongoDBHandler extends HttpServlet implements SaffronDataSource {

    final String host;
    final int port;
    String dbName;
    final String collectionName;
    MongoClient mongoClient;
    final MongoDatabase database;
    final MongoCollection runCollection;
    final String RUN_IDENTIFIER = "run";
    final String RUN_DATE = "run_date";
    
    final MongoCollection termsCollection;
    final MongoCollection termsCorrespondenceCollection;
    final MongoCollection termsExtractionCollection;

    final MongoCollection conceptsCollection;
    final String CONCEPT_IDENTIFIER = "id";
    final String CONCEPT_PREFERRED_TERM_STRING = "preferred_term";
    final String CONCEPT_SYNONYM_LIST = "synonyms";

    final MongoCollection authorsCollection;
    final String AUTHOR_IDENTIFIER = "id";
    final String AUTHOR_NAME = "name";
    final String AUTHOR_NAME_VARIANTES = "name_variants";
    
    final MongoCollection authorTermsCollection;
    final String AUTHOR_TERM_ID = "_id";
    final String AUTHOR_TERM_AUTHOR_ID = "author_id";
    final String AUTHOR_TERM_MATCHES = "matches";
    final String AUTHOR_TERM_OCCURRENCES = "occurrences";
    final String AUTHOR_TERM_SCORE = "score";
    final String AUTHOR_TERM_TERM_ID = "term_id";
    final String AUTHOR_TERM_TFIRF = "tfirf";
    final String AUTHOR_TERM_PAPER_COUNT = "paper_count";
    final String AUTHOR_TERM_RESEARCHER_SCORE = "researcher_score";  
    
    final MongoCollection termsSimilarityCollection;
    final MongoCollection authorSimilarityCollection;
    final MongoCollection taxonomyCollection;
    final MongoCollection knowledgeGraphCollection;
    final MongoCollection corpusCollection;

    private final Map<String, MongoDBHandler.SaffronDataImpl> data = new HashMap<>();

    public final String type = "mongodb";

    static String mongoHost = System.getenv("MONGO_HOST");
    static String mongoPort = System.getenv("MONGO_PORT");
    static String mongoDbName = System.getenv("MONGO_DB_NAME");

    private static class SaffronDataImpl {

        private Taxonomy taxonomy;
        private KnowledgeGraph knowledgeGraph;
        private List<AuthorAuthor> authorSim;
        private List<TermTerm> termSim;
        private List<AuthorTerm> authorTerms;
        private List<DocumentTerm> docTerms;
        private HashMap<String, Term> terms;
        private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
        private HashMap<String, List<TermTerm>> termByTerm1, termByTerm2;
        private HashMap<String, List<DocumentTerm>> docByTerm, termByDoc;
        private HashMap<String, List<AuthorTerm>> authorByTerm, termByAuthor;
        private List<String> termsSorted;
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

        public void setKnowledgeGraph(KnowledgeGraph knowledgeGraph) {
            this.knowledgeGraph = knowledgeGraph;
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

        public List<AuthorTerm> getAuthorTerms() {
            return authorTerms;
        }

        public void setAuthorTerms(Collection<AuthorTerm> authorTerms) {
            authorByTerm = new HashMap<>();
            termByAuthor = new HashMap<>();
            for (AuthorTerm at : authorTerms) {
                if (!authorByTerm.containsKey(at.getTermId())) {
                    authorByTerm.put(at.getTermId(), new ArrayList<AuthorTerm>());
                }
                authorByTerm.get(at.getTermId()).add(at);
                if (!termByAuthor.containsKey(at.getAuthorId())) {
                    termByAuthor.put(at.getAuthorId(), new ArrayList<AuthorTerm>());
                }
                termByAuthor.get(at.getAuthorId()).add(at);
            }
            this.authorTerms = new ArrayList<>(authorTerms);
        }

        public List<AuthorTerm> getAuthorByTerm(String term) {
            if (authorByTerm == null){
                authorByTerm = new HashMap<>();
            }
            List<AuthorTerm> ats = authorByTerm.get(term);
            return ats == null ? Collections.EMPTY_LIST : ats;
        }

        public List<Author> authorTermsToAuthors(List<AuthorTerm> ats) {
            List<Author> authors = new ArrayList<>();
            for (AuthorTerm at : ats) {
                Author a = getAuthor(at.getAuthorId());
                if (a != null) {
                    authors.add(a);
                }
            }
            return authors;
        }

        public List<AuthorTerm> getTermByAuthor(String author) {
            List<AuthorTerm> ats = termByAuthor.get(author);
            return ats == null ? Collections.EMPTY_LIST : ats;
        }

        public List<DocumentTerm> getDocTerms() {
            return docTerms;
        }

        public void setDocTerms(List<DocumentTerm> docTerms) {
            docByTerm = new HashMap<>();
            termByDoc = new HashMap<>();
            for (DocumentTerm dt : docTerms) {
                if (!docByTerm.containsKey(dt.getTermString())) {
                    docByTerm.put(dt.getTermString(), new ArrayList<DocumentTerm>());
                }
                docByTerm.get(dt.getTermString()).add(dt);
                if (!termByDoc.containsKey(dt.getDocumentId())) {
                    termByDoc.put(dt.getDocumentId(), new ArrayList<DocumentTerm>());
                }
                termByDoc.get(dt.getDocumentId()).add(dt);
            }
            this.docTerms = docTerms;
        }

        public List<org.insightcentre.nlp.saffron.data.Document> getDocByTerm(String term) {
            final List<DocumentTerm> dts = docByTerm.get(term);
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

        public List<DocumentTerm> getTermByDoc(String doc) {
            List<DocumentTerm> dts = termByDoc.get(doc);
            if (dts == null) {
                return Collections.EMPTY_LIST;
            } else {
                return dts;
            }
        }

        public Collection<String> getTopTerms(int from, int to) {
            if (from < termsSorted.size() && to <= termsSorted.size()) {
                return termsSorted.subList(from, to);
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public Term getTerm(String term) {
            return terms.get(term);
        }

        public Collection<Term> getTerms() {
            return terms == null ? Collections.EMPTY_LIST : terms.values();
        }

        public void setTerms(Collection<Term> _terms) {
            this.terms = new HashMap<>();
            this.termsSorted = new ArrayList<>();
            for (Term t : _terms) {
                this.terms.put(t.getString(), t);
                this.termsSorted.add(t.getString());
            }
            this.termsSorted.sort(new Comparator<String>() {
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

        public List<TermTerm> getTermSim() {
            return termSim;
        }

        public void setTermSim(List<TermTerm> termSim) {
            termByTerm1 = new HashMap<>();
            termByTerm2 = new HashMap<>();
            for (TermTerm tt : termSim) {
                if (!termByTerm1.containsKey(tt.getTerm1())) {
                    termByTerm1.put(tt.getTerm1(), new ArrayList<TermTerm>());
                }
                termByTerm1.get(tt.getTerm1()).add(tt);
                if (!termByTerm2.containsKey(tt.getTerm2())) {
                    termByTerm2.put(tt.getTerm2(), new ArrayList<TermTerm>());
                }
                termByTerm2.get(tt.getTerm2()).add(tt);
            }
            this.termSim = termSim;
        }

        public List<TermTerm> getTermByTerm1(String term1, List<String> _ignore) {
            Set<String> ignore = _ignore == null ? new HashSet<>() : new HashSet<>(_ignore);
            List<TermTerm> tt = termByTerm1.get(term1);
            if (tt != null) {
                Iterator<TermTerm> itt = tt.iterator();
                while (itt.hasNext()) {
                    if (ignore.contains(itt.next().getTerm2())) {
                        itt.remove();
                    }
                }
                return tt;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public List<TermTerm> getTermByTerm2(String term2) {
            List<TermTerm> tt = termByTerm2.get(term2);
            return tt == null ? Collections.EMPTY_LIST : tt;
        }

        /**
         * Is the Saffron data available. If this is false the getters of this
         * class may return null;
         *
         * @return true if the code is loaded
         */
        public boolean isLoaded() {
            return taxonomy != null && authorSim != null && termSim != null
                    && authorTerms != null && docTerms != null && terms != null
                    && corpus != null;
        }

        public void setCorpus(JSONObject corpus) {
            this.corpus = new HashMap<>();
            this.corpusByAuthor = new HashMap<>();
            this.authors = new HashMap<>();


            JSONArray documentArray = null;
            try {
                documentArray = (JSONArray) corpus.get("documents");
            } catch (Exception e) {
                documentArray = new JSONArray();
            }

            ArrayList<org.insightcentre.nlp.saffron.data.Document> docData = new ArrayList<>();
            if (documentArray != null) {
                for (int i=0;i<documentArray.length();i++){
                    JSONObject obj = documentArray.getJSONObject(i);
                    String contents = "";
                    if (obj.has("contents"))
                        contents = obj.getString("contents");
                    SaffronPath path = new SaffronPath();
                    path.setPath(obj.getString("id"));
                    org.insightcentre.nlp.saffron.data.Document doc = new org.insightcentre.nlp.saffron.data.Document(
                            path, obj.getString("id"), null, obj.getString("name"), obj.getString("mime_type"),
                            Collections.EMPTY_LIST, Collections.EMPTY_MAP, contents,
                            obj.has("date") ? org.insightcentre.nlp.saffron.data.Document.parseDate(obj.getString("date")) : null);

                    docData.add(doc);
                }
            }
            for (org.insightcentre.nlp.saffron.data.Document d : docData) {
                this.corpus.put(d.id, d);
                for (Author a : d.getAuthors()) {
                    if (!corpusByAuthor.containsKey(a.id)) {
                        corpusByAuthor.put(a.id, new ArrayList<>());
                    }
                    corpusByAuthor.get(a.id).add(d);
                    if (!authors.containsKey(a.id)) {
                        authors.put(a.id, a);
                    }
                }
            }
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

        public List<TermAndScore> getTaxoChildrenScored(String term_string) {
            IntList il = taxoMap.get(term_string);
            if (il != null) {
                Taxonomy t = taxoNavigate(taxonomy, il);
                List<TermAndScore> children = new ArrayList<>();
                for (Taxonomy t2 : t.children) {
                    children.add(new TermAndScore(t2.root, t2.linkScore));
                }
                return children;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        private void updateTermName(String term, String newTerm, Status status) {
            Term t = terms.get(term);
            if (t != null) {
                for (AuthorTerm at : authorTerms) {
                    if (at.getTermId().equals(term)) {
                        at.setTermId(newTerm);
                    }
                }
                for (DocumentTerm dt : docTerms) {
                    if (dt.getTermString().equals(term)) {
                        dt.setTermString(newTerm);
                    }
                }
                for (TermTerm tt : termSim) {
                    if (tt.getTerm1().equals(term)) {
                        tt.setTerm1(newTerm);
                    }
                    if (tt.getTerm2().equals(term)) {
                        tt.setTerm2(newTerm);
                    }
                }
                updateTermNameInTaxonomy(taxonomy, term, newTerm);
                t.setString(newTerm);
                t.setStatus(status);
            }
        }

        private void updateTermNameInTaxonomy(Taxonomy taxo, String term, String newTerm) {
            if(taxo.root.equals(term)) {
                taxo.root = newTerm;
            } else {
                for(Taxonomy child : taxo.getChildren()) {
                    updateTermNameInTaxonomy(child, term, newTerm);
                }
            }
        }
    }

    public static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    public MongoDBHandler() {
        this.host = mongoHost;
        this.port = new Integer(mongoPort);
        this.dbName = mongoDbName;
        this.collectionName = "saffron_runs";
        MongoClientOptions options = MongoClientOptions.builder().cursorFinalizerEnabled(false).build();

        this.mongoClient = new MongoClient(host, port);
        this.database = mongoClient.getDatabase(dbName);
        this.runCollection = database.getCollection(collectionName);
        this.termsCollection = database.getCollection(collectionName + "_terms");
        this.termsCorrespondenceCollection = database.getCollection(collectionName + "_terms_correspondence");
        this.termsExtractionCollection = database.getCollection(collectionName + "_terms_extraction");
        this.conceptsCollection = database.getCollection(collectionName + "_concepts");
        this.authorsCollection = database.getCollection(collectionName + "_authors");
        this.authorTermsCollection = database.getCollection(collectionName + "_author_terms");
        this.termsSimilarityCollection = database.getCollection(collectionName + "_terms_similarity");
        this.authorSimilarityCollection = database.getCollection(collectionName + "_author_similarity");
        this.taxonomyCollection = database.getCollection(collectionName + "_taxonomy");
        this.corpusCollection = database.getCollection(collectionName + "_corpus");
        this.knowledgeGraphCollection = database.getCollection(collectionName + "_knowledge_graph");

        this.initialiseInMemoryDatabase();
    }

    public String getMongoUrl() {
        StringBuilder url = new StringBuilder();
        url.append("mongodb://").append(this.host).append(":").append(this.port).append("/").append(this.dbName);

        return url.toString();
    }

    private void initialiseInMemoryDatabase() {
        try {
            List<SaffronRun> runs = this.getAllRuns();
            for (SaffronRun doc : runs) {
                this.fromMongo(doc.id);
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

        File termSimFile = new File(directory, "term-sim.json");
        if (!termSimFile.exists()) {
            //Enable compatibility with version 3.3
            termSimFile = new File(directory, "topic-sim.json");
            if (!termSimFile.exists()) {
                throw new FileNotFoundException("Could not find term-sim.json");
            }
        }

        File authorTermFile = new File(directory, "author-terms.json");
        if (!authorTermFile.exists()) {
            //Enable compatibility with version 3.3
            authorTermFile = new File(directory, "author-topics.json");
            if (!authorTermFile.exists()) {
                throw new FileNotFoundException("Could not find author-terms.json");
            }
        }

        File docTermsFile = new File(directory, "doc-terms.json");
        if (!docTermsFile.exists()) {
            //Enable compatibility with version 3.3
            docTermsFile = new File(directory, "doc-topics.json");
            if (!docTermsFile.exists()) {
                throw new FileNotFoundException("Could not find doc-terms.json");
            }
        }

        File termsFile = new File(directory, "terms.json");
        if (!termsFile.exists()) {
            //Enable compatibility with version 3.3
            termsFile = new File(directory, "topics.json");
            if (!termsFile.exists()) {
                throw new FileNotFoundException("Could not find terms.json");
            }
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
        this.setTermSim(runId,  mapper.readValue(termSimFile,
                tf.constructCollectionType(List.class, TermTerm.class)));
        this.setAuthorTerms(runId, mapper.readValue(authorTermFile,
                tf.constructCollectionType(List.class, AuthorTerm.class)));
        this.setDocTerms(runId, mapper.readValue(docTermsFile,
                tf.constructCollectionType(List.class, DocumentTerm.class)));
        this.setTerms(runId, mapper.readValue(termsFile,
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

        Iterable<org.bson.Document> termSimDocs = this.getTermsSimilarity(runId);
        JSONArray termSimDocsArray = new JSONArray();
        for (org.bson.Document doc : termSimDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            termSimDocsArray.put(jsonObj);
        }
        saffron.setTermSim((List<TermTerm>) mapper.readValue(termSimDocsArray.toString(),
                tf.constructCollectionType(List.class, TermTerm.class)));

        Iterable<org.bson.Document> authorTermsDocs = this.getAuthorTerms(runId);
        JSONArray authorTermsDocsArray = new JSONArray();
        for (org.bson.Document doc : authorTermsDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            authorTermsDocsArray.put(jsonObj);
        }
        saffron.setAuthorTerms((List<AuthorTerm>) mapper.readValue(authorTermsDocsArray.toString(),
                tf.constructCollectionType(List.class, AuthorTerm.class)));


        Iterable<org.bson.Document> docTermsDocs = this.getDocumentTermCorrespondence(runId);
        JSONArray docTermsArray = new JSONArray();
        for (org.bson.Document doc : docTermsDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());

            docTermsArray.put(jsonObj);
        }
        saffron.setDocTerms((List<DocumentTerm>) mapper.readValue(docTermsArray.toString(),
                tf.constructCollectionType(List.class, DocumentTerm.class)));

        Iterable<org.bson.Document> termsDocs = this.getTerms(runId);
        JSONArray jsonTermsArray = new JSONArray();
        for (org.bson.Document doc : termsDocs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            jsonTermsArray.put(jsonObj);
        }
        saffron.setTerms((List<Term>) mapper.readValue(jsonTermsArray.toString(),
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
        final ObjectMapper mapper = new ObjectMapper();

        try {
            String json =  mapper.writeValueAsString(config);
            this.data.put(id, saffron);
            Document document = new Document();
            document.put("id", id);
            document.put("run_date", date);
            document.put("config", json);
            this.runCollection.insertOne(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public List<SaffronRun>  getAllRuns() {
        try {
            List<SaffronRun> runList = new ArrayList<>();
            FindIterable<Document> docs = MongoUtils.getDocs(this);
            for (org.bson.Document doc : docs) {
                String id = doc.getString("id");
                Date runDate = doc.getDate("run_date");
                String config = doc.getString("config");
                SaffronRun run = new SaffronRun(id, runDate, config);
                runList.add(run);
            }
            return runList;
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
            Iterable<org.bson.Document> docs = getRunFromMongo(runId, runCollection);
            for (org.bson.Document doc : docs) {
                jsonObj = new JSONObject(doc.toJson());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObj.toString();
    }

    @Override
    public void updateRun(String runId, String statusMessage, JSONObject jsonObj, String status) {
        JSONArray currentStatus;
        ObjectMapper mapper = new ObjectMapper();
        try {
            currentStatus = (JSONArray) jsonObj.get("execution_status");
        } catch (Exception e) {
            currentStatus = new JSONArray();
        }

        try {
            Bson conditionSingle = Filters.and(Filters.eq("id", runId));
            FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
            findOptions.upsert(true);
            findOptions.returnDocument(ReturnDocument.AFTER);
            if (currentStatus.length() > 0) {
                Document toPut = new Document();
                List<Map> records = mapper.readValue(currentStatus.toString(), List.class);
                List<Map> newRecords = new ArrayList<>();
                if (status.equals("completed")) {
                    for (Map record : records) {
                        if(record.get("stage").toString().equals(statusMessage) && status.equals("completed")) {
                            record.put("end_time", new Date().toString());
                            record.put("status", status);
                            newRecords.add(record);
                        } else {
                            newRecords.add(record);
                        }
                    }
                } else {
                    Map<String, Object> newRecord = new LinkedHashMap<>();
                    newRecord.put("start_time", new Date().toString());
                    newRecord.put("end_time", "");
                    newRecord.put("stage", statusMessage);
                    newRecord.put("status", status);
                    newRecords.add(newRecord);
                    newRecords.addAll(records);
                }
                toPut.put("execution_status", newRecords);
                runCollection.findOneAndUpdate(conditionSingle, new Document("$set", toPut), findOptions);
            } else {
                List<Map> entries = new ArrayList<>();
                Map<String, Object> records = new LinkedHashMap<>();
                Document toPut = new Document();
                records.put("start_time", new Date().toString());
                records.put("end_time", "");
                records.put("stage", statusMessage);
                records.put("status", status);
                entries.add(records);
                toPut.put("execution_status", entries);
                runCollection.findOneAndUpdate(conditionSingle, new Document("$set", toPut), findOptions);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static FindIterable getRunFromMongo(String runId, MongoCollection runCollection) {
        return runCollection.find(and(eq("id", runId)));
    }

    public FindIterable<Document> getTermExtraction(String runId) {
        return termsExtractionCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getTermExtractionForTerm(String runId, String termId) {
        return termsExtractionCollection.find(and(eq("run", runId), eq("termId", termId)));
    }

    //TODO: Create final variable with names of attributes in each collection
    public boolean addTermExtraction(String id, Date date, Set<Term> res) {
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
            termsExtractionCollection.insertOne(document);
        });
        return true;
    }



    public boolean addDocumentTermCorrespondence(String id, Date date, List<DocumentTerm> terms) {
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
            termsCorrespondenceCollection.insertOne(document);
        });
        return true;
    }



    public FindIterable<Document> getDocumentTermCorrespondence(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.termsCorrespondenceCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getDocumentTermCorrespondenceForTerm(String runId, String termId) {

        return termsCorrespondenceCollection.find(and(eq("run", runId), eq("term", termId)));
    }


    public FindIterable<Document> getDocumentTermCorrespondenceForDocument(String runId, String docId) {
        return termsCorrespondenceCollection.find(and(eq("run", runId), eq("document_id", docId)));
    }

    public boolean addTerms(String id, Date date, List<Term> terms) {
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
            termsCollection.insertOne(document);
        });



        return true;
    }

    public FindIterable<Document> getTerms(String runId) {
        FindIterable<Document> docs = MongoUtils.getTermsFromMongo(runId, this);
        return docs;
    }


    public void deleteTerm(String runId, String term) {

        this.removeTermFromConcepts(runId, term);

        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("run", runId);
        updateFields.append("term", term);
        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);
        termsCollection.findOneAndDelete(and(eq("run", runId), (eq("term", term))));
    }

    /*
     * Concept Manipulation
     */

    /**
     * Removes a relationship between a term and multiple concepts
     *
     * @author Bianca Pereira
     *
     * @param runId - the run to be manipulated
     * @param term - the string of the term to be removed
     */
    //FIXME Due to the way how relations between terms and concepts is instantiated, the delete
    // operation is inefficient, requirement going through the whole database of concepts
    public void removeTermFromConcepts(String runId, String term) {
        for(Concept concept: getAllConcepts(runId)) {

            try {
                if (concept.getPreferredTerm().equals(term)) {
                    // If term is a preferred term, then choose a random synonym to become
                    // a preferred term, or remove the concept if no synonym is available
                    if (concept.getSynonyms() == null || concept.getSynonyms().size() == 0)
                        this.removeConcept(runId, concept.getId());
                    else {
                        concept.setPreferredTerm(concept.getSynonyms().iterator().next());
                        this.updateConcept(runId, concept);
                    }
                } else if (concept.getSynonymsStrings().contains(term)) {
                    // If term is not a preferred term, just remove it from the list of synonyms
                    Set<Term> synonyms = concept.getSynonyms();
                    Term toBeRemoved = null;
                    for(Term toRemove: synonyms){
                        if (toRemove.getString().equals(term)) {
                            toBeRemoved = toRemove;
                            break;
                        }
                    }
                    synonyms.remove(toBeRemoved);
                    concept.setSynonyms(synonyms);
                    this.updateConcept(runId, concept);
                }
            } catch (ConceptNotFoundException | TermNotFoundException e) {
                //Include logging here
                throw new RuntimeException("An error has occurred while removing term-concept relationships",e);
            }
        }
    }

    /**
     * Retrieves all concepts from the Database for a given runId
     *
     * @author Bianca Pereira
     *
     * @param runId - the run to be retrieved
     * @return - An {@link ArrayList} of {@link Concept} objects
     */
    public List<Concept> getAllConcepts(String runId){
        Bson filter = eq(RUN_IDENTIFIER, runId);
        List<Concept> concepts = this.findConcepts(filter);

        return concepts;
    }

    /**
     * Retrieves a concept with a given id
     *
     * @author Bianca Pereira
     *
     * @param runId - the Id of the run
     * @param conceptId - the identifier of the concept to be retrieved
     * @return A {@link Concept} with the provided id or {@code null}
     */
    public Concept getConcept(String runId, String conceptId){

        Bson filter = and(eq(RUN_IDENTIFIER, runId),eq(CONCEPT_IDENTIFIER, conceptId));
        List<Concept> concepts = this.findConcepts(filter);

        if(concepts.size() > 0)
            return concepts.get(0);
        else
            return null;
    }

    /**
     * Retrieves a list of concepts with a specified preferred term
     *
     * @author Bianca Pereira
     *
     * @param runId - the Id of the run
     * @param preferredTermString - the preferred term string
     * @return A {@link List} of {@link Concept} objects
     */
    public List<Concept> getConceptsByPreferredTermString(String runId, String preferredTermString){

        Bson filter = and(eq(RUN_IDENTIFIER, runId),eq(CONCEPT_PREFERRED_TERM_STRING, preferredTermString));
        List<Concept> concepts = this.findConcepts(filter);
        return concepts;
    }

    /**
     * Search for concepts in the Mongo Database according to the provided filters
     *
     * @author Bianca Pereira
     *
     * @param filter - the filter for the search
     * @return the result of the search as a {link List} with {link Concept} objects
     */
    private List<Concept> findConcepts(Bson filter) {
        Iterable<Document> dbConcepts = conceptsCollection.find(filter);

        List<Concept> concepts = new ArrayList<Concept>();
        ObjectMapper mapper = new ObjectMapper();

        for(Document dbDoc: dbConcepts) {
            try {
                concepts.add(mapper.readValue(dbDoc.toJson(), Concept.class));
            } catch (Exception e) {
                throw new RuntimeException("Error retrieving concepts from Mongo Database", e);
            }
        }

        return concepts;
    }

    /**
     * Add a set of concepts in the database
     *
     * @author Bianca Pereira
     *
     * @param runId - the identifier of the run
     * @param concepts - a {@link List} of {@link Concept} to be saved in the database.
     *     Each concept must contain a unique id, a valid preferred term and synonyms.
     *
     * @throws {@link RuntimeException} if the a {@link Concept} with same id
     *     already exists in the database
     * @throws {@link TermNotFoundException} if the preferred term or any of the synonyms
     *     for a given concept is not already in the database.
     */
    public void addConcepts(String runId, List<Concept> concepts) {
        if (concepts != null) {
            for(Concept concept: concepts) {
                try {
                    this.addConcept(runId, concept);
                } catch (TermNotFoundException e) {
                    //TODO Include logging!!!!!!
                    // The term X could not be found in the database, Skipping concept Y
                    continue;
                }
            }
        }
    }

    /**
     * Adds a new concept in the database.
     *
     * @author Bianca Pereira
     *
     * @param runId - the identifier of the run
     * @param conceptToBeAdded - the concept to be saved in the database.
     *     It must contain a unique id, a valid preferred term and synonyms.
     * @throws TermNotFoundException when one of the terms was not found in the database
     *
     * @throws {@link RuntimeException} if the a {@link Concept} with same id
     *     already exists in the database
     * @throws {@link TermNotFoundException} if the preferred term or any of the synonyms
     *     is not already in the database.
     */
    public void addConcept(String runId, Concept conceptToBeAdded) throws TermNotFoundException{
        /*
         * 1 - Verify if the preferredTerm exists. If not, throw object not found
         * 2 - Verify if each synonym exists. If not, throw object not found
         * 3 - Save the object in the database
         */
        if (this.getConcept(runId, conceptToBeAdded.getId()) != null)
            throw new RuntimeException("A concept with same id already exists in the database. id: " + conceptToBeAdded.getId());

        if (this.getTerm(runId, conceptToBeAdded.getPreferredTermString()) == null)
            throw new TermNotFoundException(conceptToBeAdded.getPreferredTerm());

        for (Term synonym: conceptToBeAdded.getSynonyms()) {
            if (this.getTerm(runId, synonym.getString()) == null)
                throw new TermNotFoundException(synonym);
        }

        Bson dbObject = this.createBsonDocumentForConcept(runId, conceptToBeAdded);
        conceptsCollection.insertOne(dbObject);
    }

    /**
     * Updates a concept already in the database.
     *
     * @author Bianca Pereira
     *
     * @param runId - the identifier of the run
     * @param conceptToBeUpdated - the concept to be updated in the database.
     *     It must contain a unique id, a valid preferred term and synonyms.
     * @throws ConceptNotFoundException - if {@link Concept} with same id does not exist in the database
     * @throws TermNotFoundException - if {@link Term} given as preferred string
     *  does not exist in the database
     *
     * @throws {@link ConceptNotFoundException} if there is no {@link Concept} with same id
     *     in the database.
     * @throws {@link TermNotFoundException} if the preferred term or any of the synonyms
     *     is not already in the database.
     */
    public void updateConcept(String runId, Concept conceptToBeUpdated)
            throws ConceptNotFoundException, TermNotFoundException{
        /*
         * 1 - Verify if an object with that id already exists. If not, throw an exception.
         * 2 - Verify if the preferredTerm exists. If not, throw object not found
         * 3 - Verify if each synonym exists. If not, throw object not found
         * 4 - Save the object in the database
         */

        if (this.getConcept(runId, conceptToBeUpdated.getId()) == null)
            throw new ConceptNotFoundException(conceptToBeUpdated);

        if (this.getTerm(runId, conceptToBeUpdated.getPreferredTermString()) == null)
            throw new TermNotFoundException(conceptToBeUpdated.getPreferredTerm());

        for (Term synonym: conceptToBeUpdated.getSynonyms()) {
            if (this.getTerm(runId, synonym.getString()) == null)
                throw new TermNotFoundException(synonym);
        }

        Bson dbObject = this.createBsonDocumentForConcept(runId, conceptToBeUpdated);
        conceptsCollection.updateOne(eq(CONCEPT_IDENTIFIER,conceptToBeUpdated.getId()), dbObject);
    }

    /**
     * Create a {@link Bson} for a {@link Concept}.
     *
     * @param concept - the concept to be converted
     * @return the {@link Bson} object
     */
    private Document createBsonDocumentForConcept(String runId, Concept concept) {
        Document doc = new Document(CONCEPT_IDENTIFIER, concept.getId())
                .append(CONCEPT_PREFERRED_TERM_STRING, concept.getPreferredTermString())
                .append(RUN_IDENTIFIER, runId);

        if(concept.getSynonyms() != null && concept.getSynonyms().size() >0) {
            doc.append(CONCEPT_SYNONYM_LIST, Arrays.asList(concept.getSynonymsStrings()));
        }

        return doc;
    }

    /**
     * Remove a concept from the database.
     *
     * @author Bianca Pereira
     *
     * @param runId - the identifier of the run
     * @param conceptToBeUpdated - the concept to be removed from the database.
     * @throws ConceptNotFoundException when a {@link Concept} with this id does not exist
     *
     * @throws {@link ConceptNotFoundException} if there is no {@link Concept} with same id
     *     in the database.
     */
    public void removeConcept(String runId, String conceptToBeUpdated) throws ConceptNotFoundException{
        /*
         * 1 - Verify if an object with that id already exists. If not, throw an exception.
         * 2 - Remove the object
         */
        if (this.getConcept(runId, conceptToBeUpdated) == null)
            throw new ConceptNotFoundException(new Concept.Builder(conceptToBeUpdated, "").build());

        conceptsCollection.deleteOne(eq(CONCEPT_IDENTIFIER,conceptToBeUpdated));
    }
    
    /**
     * Retrieves all authors from the Database for a given runId
     *
     * @author Bianca Pereira
     *
     * @param runId - the run to be retrieved
     * @return - An {@link ArrayList} of {@link Concept} objects
     */
    public Iterable<Author> getAllAuthors(String runId){
        Bson filter = eq(RUN_IDENTIFIER, runId);
        List<Author> authors = this.findAuthors(filter);

        return authors;
    }
    
    /**
     * Retrieves an author with a given id
     *
     * @author Bianca Pereira
     *
     * @param runId - the Id of the run
     * @param authorId - the identifier of the author to be retrieved
     * @return An {@link Author} with the provided id or {@code null}
     */
    public Author getAuthor(String runId, String authorId){

        Bson filter = and(eq(RUN_IDENTIFIER, runId),eq(AUTHOR_IDENTIFIER, authorId));
        List<Author> authors = this.findAuthors(filter);

        if(authors.size() > 0)
            return authors.get(0);
        else
            return null;
    }
    
    /**
     * Search for authors in the Mongo Database according to the provided filters
     *
     * @author Bianca Pereira
     *
     * @param filter - the filter for the search
     * @return the result of the search as a {link List} with {link Author} objects
     */
    private List<Author> findAuthors(Bson filter) {
        Iterable<Document> dbAuthors = authorsCollection.find(filter);

        List<Author> authors = new ArrayList<Author>();
        ObjectMapper mapper = new ObjectMapper();

        Author author;
        for(Document dbDoc: dbAuthors) {
            try {
            	author = mapper.readValue(dbDoc.toJson(), Author.class);
            	if (author!=null)
            		authors.add(author);
            } catch (Exception e) {
                throw new RuntimeException("Error retrieving authors from Mongo Database", e);
            }
        }

        return authors;
    }
    
    /**
     * Add a set of authors in the database
     *
     * @author Bianca Pereira
     *
     * @param runId - the identifier of the run
     * @param authors - a {@link List} of {@link Author} to be saved in the database.
     *     Each author must contain a unique id and a valid name.
     *
     * @throws {@link RuntimeException} if as {@link Author} with same id
     *     already exists in the database.
     */
    public void addAuthors(String runId, List<Author> authors) {
        if (authors != null) {
            for(Author author: authors) {
                try {
                    this.addAuthor(runId, author);
                } catch (Exception e) {
                    //TODO Include logging!!!!!!
                    // The author X could not be found in the database, Skipping concept Y
                    continue;
                }
            }
        }
    }

    /**
     * Adds a new author in the database.
     *
     * @author Bianca Pereira
     *
     * @param runId - the identifier of the run
     * @param authorToBeAdded - the author to be saved in the database.
     *     It must contain a unique id and a name
     *
     * @throws {@link RuntimeException} if the an {@link Author} with same id
     *     already exists in the database or if id or name are empty or null
     */
    public void addAuthor(String runId, Author authorToBeAdded) throws Exception{
    	if (authorToBeAdded.id == null || authorToBeAdded.id.equals(""))
    		throw new RuntimeException("The author must have a non empty id");
    	
    	if (authorToBeAdded.name == null || authorToBeAdded.name.equals(""))
    		throw new RuntimeException("The author must have a non empty name");
    	
        if (this.getAuthor(runId, authorToBeAdded.id) != null)
            throw new RuntimeException("An author with same id already exists in the database. id: " + authorToBeAdded.id);

        Bson dbObject = this.createBsonDocumentForAuthor(runId, authorToBeAdded);
        authorsCollection.insertOne(dbObject);
    }
    
    /**
     * Create a {@link Bson} for a {@link Author}.
     *
     * @param author - the author to be converted
     * @return the {@link Bson} object
     */
    private Document createBsonDocumentForAuthor(String runId, Author author) {
        Document doc = new Document(AUTHOR_IDENTIFIER, author.id)
                .append(AUTHOR_NAME, author.name)
                .append(AUTHOR_NAME_VARIANTES, author.nameVariants)
                .append(RUN_IDENTIFIER, runId);

        return doc;
    }

    public boolean addAuthorTerms(String id, Date date, Collection<AuthorTerm> terms) {
        Document document = new Document();
        int[] idx = { 0 };
        terms.forEach(name -> {
            document.put(AUTHOR_TERM_ID, id + "_" + name.getTermId() + "_" + idx[0]++);
            document.put(RUN_IDENTIFIER, id);
            document.put(RUN_DATE, date);
            document.put(AUTHOR_TERM_AUTHOR_ID, name.getAuthorId());
            document.put(AUTHOR_TERM_MATCHES, name.getMatches());
            document.put(AUTHOR_TERM_OCCURRENCES, name.getOccurrences());
            document.put(AUTHOR_TERM_SCORE, name.getScore());
            document.put(AUTHOR_TERM_TERM_ID, name.getTermId());
            document.put(AUTHOR_TERM_TFIRF, name.getTfIrf());
            document.put(AUTHOR_TERM_PAPER_COUNT, name.getPaperCount());
            document.put(AUTHOR_TERM_RESEARCHER_SCORE, name.getResearcherScore());
            authorTermsCollection.insertOne(document);
        });
        return true;
    }
    
    public List<AuthorTerm> getAuthorTermRelationsPerTerm(String runId, String termId) {
    	List<AuthorTerm> result = new ArrayList<AuthorTerm>();
    	
    	FindIterable<Document> authorTermRelations = authorTermsCollection.find(and(eq(RUN_IDENTIFIER, runId), eq(AUTHOR_TERM_TERM_ID, termId)));
    	for(Document doc: authorTermRelations) {
    		result.add(this.buildAuthorTerm(doc));
    	}
    	
    	return result;
    }
    
    public List<AuthorTerm> getAuthorTermRelationsPerAuthor(String runId, String authorId) {
    	List<AuthorTerm> result = new ArrayList<AuthorTerm>();
    	
    	FindIterable<Document> authorTermRelations = authorTermsCollection.find(and(eq(RUN_IDENTIFIER, runId), eq(AUTHOR_TERM_AUTHOR_ID, authorId)));
    	for(Document doc: authorTermRelations) {
    		result.add(this.buildAuthorTerm(doc));
    	}
    	
    	return result;
    }

    private AuthorTerm buildAuthorTerm(Document doc) {
    	AuthorTerm object = new AuthorTerm();
    	
    	object.setAuthorId(doc.getString(AUTHOR_TERM_AUTHOR_ID));
    	object.setMatches(doc.getInteger(AUTHOR_TERM_MATCHES));
    	object.setOccurrences(doc.getInteger(AUTHOR_TERM_OCCURRENCES));
    	object.setPaperCount(doc.getInteger(AUTHOR_TERM_PAPER_COUNT));
    	object.setResearcherScore(doc.getDouble(AUTHOR_TERM_RESEARCHER_SCORE));
    	object.setScore(doc.getDouble(AUTHOR_TERM_SCORE));
    	object.setTermId(doc.getString(AUTHOR_TERM_TERM_ID));
    	object.setTfIrf(doc.getDouble(AUTHOR_TERM_TFIRF));
    	
    	return object;
    }
    
    public FindIterable<Document>  getAuthorTerms(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.authorTermsCollection.find(and(eq("run", runId)));

    }

    public boolean addTermsSimilarity(String id, Date date, List<TermTerm> termSimilarity) {
        Document document = new Document();
        int[] idx = { 0 };
        for (TermTerm term : termSimilarity) {
            document.put("_id", id + "_" + term.getTerm1() + "_" + term.getTerm2() + "_" + idx[0]++);
            document.put("run", id);
            document.put("run_date", date);
            document.put("term1_id", term.getTerm1());
            document.put("term2_id", term.getTerm2());
            document.put("similarity", term.getSimilarity());
            termsSimilarityCollection.insertOne(document);
        }
        return true;
    }

    public FindIterable<Document> getTermsSimilarity(String runId) {
        Document document = new Document();
        document.put("run", runId);
        return this.termsSimilarityCollection.find(and(eq("run", runId)));
    }

    public FindIterable<Document> getTermsSimilarityBetweenTerms(String runId, String term1, String term2) {
        Document document = new Document();
        document.put("run", runId);
        return this.termsSimilarityCollection.find(and(eq("run", runId), eq("term1_id", term1), eq("term2", term2)));
    }

    public FindIterable<Document> getTermsSimilarityForTerm(String runId, String term) {
        Document document = new Document();
        document.put("run", runId);
        return this.termsSimilarityCollection.find(and(eq("run", runId), eq("term1_id", term)));
    }

    public boolean addAuthorSimilarity(String id, Date date, List<AuthorAuthor> authorSim) {
        Document document = new Document();

        authorSim.forEach(name -> {
            document.put("_id", id + "_" + name.getAuthor1_id() + "_" + name.getAuthor2_id());
            document.put("run", id);
            document.put("run_date", date);
            document.put("author1_id", name.getAuthor1_id());
            document.put("author2_id", name.getAuthor2_id());
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

    @Override
    public List<AuthorAuthor> getAuthorSimilarity(String runId, String authorId) {
        Iterable<Document> docs = this.authorSimilarityCollection.find(and(eq("run", runId), eq("author1_id", authorId)));
        List<AuthorAuthor> aas = new ArrayList<>();
        for(Document d : docs) {
            aas.add(new AuthorAuthor(d.getString("author1_id"), d.getString("author2_id"), 
                    d.getDouble("similarity"), d.getString("run"), null, d.getString("_id")));            
        }
        return aas;
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


    @Override
    public boolean addPartonomy(String id, Date date, Partonomy graph) {
        return false;
    }

    public boolean addKnowledgeGraph(String id, Date date, KnowledgeGraph knowledgeGraph) {

        ObjectMapper mapper = new ObjectMapper();


        try{
            Document doc = Document.parse( mapper.writeValueAsString(knowledgeGraph) );
            doc.append("id", id);
            doc.append("date", date);
            this.knowledgeGraphCollection.insertOne(doc);
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
        Taxonomy graph = new Taxonomy("", 0, 0, new ArrayList<>(), Status.none);
        FindIterable<Document> docs = MongoUtils.getTaxonomyFromMongo(runId, this);
        graph = TaxonomyUtils.getTaxonomyFromDocs(docs, graph);
        return graph;
    }

    @Override
    public Partonomy getPartonomy(String runId) {
        Partonomy partonomy = null;
        FindIterable<Document> docs = MongoUtils.getPartonomyFromMongo(runId, this);
        for (Document doc : docs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            try {
                JSONObject partonomyComponents = (JSONObject)jsonObj.get("partonomy");
                JSONArray partonomyComponentsChildren = (JSONArray)partonomyComponents.get("components");
                ArrayList<Taxonomy> listData = new ArrayList<Taxonomy>();

                if (partonomyComponents != null) {
                    for (int i=0;i<partonomyComponentsChildren.length();i++){
                        JSONObject taxo = (JSONObject)partonomyComponentsChildren.get(i);
                        Taxonomy taxonomy = Taxonomy.fromJsonString(taxo.toString());
                        listData.add(taxonomy);
                    }
                }
                partonomy = new Partonomy(listData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return partonomy;
    }

    @Override
    public KnowledgeGraph getKnowledgeGraph(String runId)  {
        KnowledgeGraph graph = new KnowledgeGraph();
        FindIterable<Document> docs = MongoUtils.getKnowledgeGraphFromMongo(runId, this);
        for (Document doc : docs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            try {
                graph = KnowledgeGraph.fromJsonString(jsonObj.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return graph;
    }


    @Override
    public List<DocumentTerm> getDocTerms(String runId) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);

        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocTerms();
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
    public List<TermAndScore> getTaxoChildrenScored(String runId, String term_string) {
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
    public List<TermTerm> getTermByTerm1(String runId, String term1, List<String> _ignore) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByTerm1(term1, _ignore);
    }

    @Override
    public List<TermTerm> getTermByTerm2(String runId, String term2) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByTerm2(term2);
    }

    @Override
    public List<AuthorTerm> getTermByAuthor(String runId, String author) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByAuthor(author);
    }

    @Override
    public List<AuthorTerm> getAuthorByTerm(String runId, String term) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorByTerm(term);
    }

    @Override
    public List<Author> authorTermsToAuthors(String runId, List<AuthorTerm> ats) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorTermsToAuthors(ats);
    }

    @Override
    public List<DocumentTerm> getTermByDoc(String runId, String doc) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByDoc(doc);
    }

    @Override
    public List<org.insightcentre.nlp.saffron.data.Document> getDocByTerm(String runId, String term) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocByTerm(term);
    }

    @Override
    public Term getTerm(String runId, String term) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTerm(term);
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
    public Collection<String> getTopTerms(String runId, int from, int to) {
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopTerms(from, to);
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
    public void setDocTerms(String runId, List<DocumentTerm> docTerms) {
        this.addDocumentTermCorrespondence(runId, new Date(), docTerms);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setDocTerms(docTerms);
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
    public void setTerms(String runId, List<Term> _terms) {
        this.addTerms(runId, new Date(), _terms);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTerms(_terms);

    }

    @Override
    public void setAuthorTerms(String runId, Collection<AuthorTerm> authorTerms) {
        this.addAuthorTerms(runId, new Date(), authorTerms);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setAuthorTerms(authorTerms);
    }

    @Override
    public void setTermSim(String runId, List<TermTerm> termSim) {
        this.addTermsSimilarity(runId, new Date(), termSim);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTermSim(termSim);
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
    public void setKnowledgeGraph(String runId, KnowledgeGraph knowledgeGraph) {
        this.addKnowledgeGraph(runId, new Date(), knowledgeGraph);
        MongoDBHandler.SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setKnowledgeGraph(knowledgeGraph);
    }

    @Override
    public void remove(String runId) {

    }


    public boolean updateTerm(String id, String term, String status) {

        try {
            Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));
            Bson update = set("status", status);


            FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
            findOptions.upsert(true);
            findOptions.returnDocument(ReturnDocument.AFTER);

            termsCollection.findOneAndUpdate(condition, update, findOptions);
            return true;
        } catch (Exception e ) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }



    public boolean updateTermSimilarity(String id, String term1, String term2, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term1", term1),
                Filters.eq("term2", term2));
        Bson update = set("status", status);



        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            termsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term1 + " from the taxonomy " + id);
            return false;
        }

    }

    public boolean updateAuthorTermName(String id, String term, String newTerm, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term_id", term));
        Bson update = combine(set("term_id", newTerm), set("termString", newTerm),
                set("originalTerm", term), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            termsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }


    public boolean updateDocumentTermName(String id, String term, String newTerm, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));
        Bson update = combine(set("term", newTerm),
                set("originalTerm", term), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            termsCorrespondenceCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }

    public boolean updateTermSimilarityName(String id, String term, String newTerm, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term1", term));
        Bson update = combine(set("term1", newTerm),
                set("originalTerm", term), set("status", status));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);
        try {
            termsSimilarityCollection.updateMany(condition, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + term + " from the taxonomy " + id);
            return false;
        }

    }



    public boolean updateTermName(String id, String term, String newTerm, String status) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));
        Bson update = combine(set("term", newTerm), set("termString", newTerm), set("originalTerm", term));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);

        termsCollection.findOneAndUpdate(condition, update, findOptions);
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
        MongoDBHandler.SaffronDataImpl saffron = data.get(datasetName);
        return saffron.getDocuments();
    }

    @Override
    public Iterable<Term> getAllTerms(String datasetName) {
        FindIterable<Document> docs = this.getTerms(datasetName);
        List<Term> returnList = new ArrayList<>();
        for (Document doc : docs) {

            String termString = doc.getString("term_string");
            int occurrences = doc.getInteger("occurences");
            int matches = doc.getInteger("matches");
            double score = doc.getDouble("score");
            String status = doc.getString("status");
            List<Term.MorphologicalVariation> mvList = new ArrayList<>();
            Term term = new Term(termString, occurrences, matches, score, mvList, status);
            returnList.add(term);
        }
        Iterable<Term> terms = returnList;
        return terms;
    }

    @Override
    public Date getDate(String doc) {
        return null;
    }

    @Override
    public List<AuthorTerm> getAllAuthorTerms(String name) {
        return null;
    }

    @Override
    public Iterable<DocumentTerm> getDocTermByTerm(String name, String termId) {
        return null;
    }

    @Override
    public Iterable<TermTerm> getAllTermSimilarities(String name) {
        FindIterable<Document> docs = this.getTerms(name);
        List<TermTerm> returnList = new ArrayList<>();
        for (Document doc : docs) {
            String term1 = doc.getString("term1_id");
            String term2 = doc.getString("term2_id");
            double similarity = doc.getDouble("similarity");
            TermTerm term = new TermTerm(term1, term2, similarity);
            returnList.add(term);
        }
        Iterable<TermTerm> terms = returnList;
        return terms;
    }

    @Override
    public Iterable<TermTerm> getTermByTerms(String name, String term1, String term2) {
        return null;
    }


    public FindIterable<Document> searchTaxonomy(String id, String term) {

        Bson condition = Filters.and(Filters.eq("run", id), Filters.eq("term", term));

        return termsCorrespondenceCollection.find(condition);
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

            GridFS gridFs = getGridFS();
            if (doc.get("documents") != null) {
                ArrayList<Document> docList = (ArrayList) doc.get("documents");
                for (Document d : docList) {
                    if (d.getString("contents") != null) {
                        GridFSInputFile gfsFile = gridFs.createFile(new ByteArrayInputStream(d.getString("contents").getBytes()));

                        gfsFile.setFilename(d.getString("id"));
                        gfsFile.put("documentType", "text");
                        gfsFile.put("taxonomyId", saffronDatasetName);
                        gfsFile.save();
                    } else {
                        GridFSInputFile gfsFile = gridFs.createFile(new ByteArrayInputStream(d.get("metadata").toString().getBytes()));

                        gfsFile.setFilename(d.getString("id"));
                        gfsFile.put("documentType", "text");
                        gfsFile.put("taxonomyId", saffronDatasetName);
                        gfsFile.save();
                    }
                }
            }
            if (getCorpusCount(saffronDatasetName) > 0)
                this.corpusCollection.findOneAndDelete(doc);
            doc.remove("documents");


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
        FindIterable<Document> docs = this.corpusCollection.find(and(eq("id", saffronDatasetName)));

        return docs;
    }

    public HashMap<String, String> getCorpusFiles(String saffronDatasetName) {
        Document document = new Document();
        document.put("id", saffronDatasetName);
        GridFS gridFs = this.getGridFS();
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("taxonomyId", saffronDatasetName);
        HashMap<String, String> map = new HashMap<>();
        List<GridFSDBFile> fs = gridFs.find(whereQuery);
        if (fs.size() > 0) {
            for (GridFSDBFile f : fs) {
                InputStreamReader isReader = new InputStreamReader(f.getInputStream());
                //Creating a BufferedReader object
                BufferedReader reader = new BufferedReader(isReader);
                StringBuffer sb = new StringBuffer();
                String str;
                try {
                    while((str = reader.readLine())!= null){
                        sb.append(str);
                    }
                    map.put(f.getFilename(), sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    public String getCorpusFile(String saffronDatasetName, String fileName) {
        Document document = new Document();
        document.put("id", saffronDatasetName);
        GridFS gridFs = this.getGridFS();
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("taxonomyId", saffronDatasetName);
        String map = "";
        List<GridFSDBFile> fs = gridFs.find(whereQuery);
        if (fs.size() > 0) {
            for (GridFSDBFile f : fs) {
                InputStreamReader isReader = new InputStreamReader(f.getInputStream());
                //Creating a BufferedReader object
                BufferedReader reader = new BufferedReader(isReader);
                StringBuffer sb = new StringBuffer();
                String str;
                try {
                    while((str = reader.readLine())!= null){
                        sb.append(str);
                    }
                    if (f.getFilename().equals(fileName))
                        map = sb.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }


    public GridFS getGridFS() {
        DB db = mongoClient.getDB(this.dbName);
        return new GridFS(db, "corpusCollection");
    }
}
