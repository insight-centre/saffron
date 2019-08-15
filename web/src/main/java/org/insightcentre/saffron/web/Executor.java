package org.insightcentre.saffron.web;

import static org.insightcentre.nlp.saffron.authors.Consolidate.applyConsolidation;
import static org.insightcentre.nlp.saffron.taxonomy.supervised.Main.loadMap;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.authors.Consolidate;
import org.insightcentre.nlp.saffron.authors.ConsolidateAuthors;
import org.insightcentre.nlp.saffron.authors.connect.ConnectAuthorTopic;
import org.insightcentre.nlp.saffron.authors.sim.AuthorSimilarity;
import org.insightcentre.nlp.saffron.config.*;
import org.insightcentre.nlp.saffron.crawler.SaffronCrawler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Model;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.documentindex.IndexedCorpus;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxonomySearch;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;
import org.insightcentre.nlp.saffron.term.TermExtraction;
import org.insightcentre.nlp.saffron.topic.topicsim.TopicSimilarity;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Files;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import java.io.FileNotFoundException;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Executor extends AbstractHandler {

    private final Map<String, SaffronData> data;
    private final File parentDirectory;
    private final Map<String, Status> statuses;
    private Corpus corpus;
    private Configuration defaultConfig;
    private final File logFile;
    //private Configuration config;

    public Executor(Map<String, SaffronData> data, File directory, File logFile) {
        this.data = data;
        this.parentDirectory = directory;
        this.statuses = new HashMap<>();
        try {
            this.defaultConfig = new ObjectMapper().readValue(new SaffronPath("${saffron.home}/models/config.json").toFile(), Configuration.class);
        } catch (IOException x) {
            this.defaultConfig = new Configuration();
            System.err.println("Could not load config.json in models folder... using default configuration (" + x.getMessage() + ")");
        }
        this.logFile = logFile;

    }

    /**
     * Initialize a new Saffron Dataset
     *
     * @param name The name
     * @return True if a new dataset was created
     */
    public boolean newDataSet(String name) {
        if (data.containsKey(name)) {
            return false;
        } else {
            data.put(name, new SaffronData());
            return true;
        }
    }

    public boolean isExecuting(String name) {
        return statuses.containsKey(name) && statuses.get(name).stage > 0 && !statuses.get(name).completed;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest hsr,
            HttpServletResponse response) throws IOException, ServletException {
        try {
            if ("/execute".equals(target)) {
                String name = hsr.getParameter("name");
                doExecute(name, response, baseRequest, hsr);
            } else if (corpus != null && (target.startsWith("/execute/advanced/"))) {
                final String saffronDatasetName = target.substring("/execute/advanced/".length());
                if (doAdvancedExecute(hsr, saffronDatasetName, response, baseRequest)) {
                    return;
                }

            } else if (target.startsWith("/api/v1/run/rerun")) {
                final String saffronDatasetName = target.substring("/api/v1/run/rerun/".length());
                doRerun(saffronDatasetName, response, baseRequest);
            } else if ("/execute/status".equals(target)) {
                String saffronDatasetName = hsr.getParameter("name");
                Status status = getStatus(saffronDatasetName);
                if (status != null) {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(response.getWriter(), statuses.get(saffronDatasetName));
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "No executing run: " + saffronDatasetName);
                    baseRequest.setHandled(true);
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    public Status getStatus(String saffronDatasetName) throws IOException {
        return statuses.get(saffronDatasetName);
    }

    private void doRerun(final String saffronDatasetName, HttpServletResponse response, Request baseRequest) throws IOException, FileNotFoundException, NumberFormatException, JSONException {
        Status _status = makeStatus();
        _status.name = saffronDatasetName;
        statuses.put(saffronDatasetName, _status);
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        FileReader reader = new FileReader(new File("static/executing.html"));
        Writer writer = new StringWriter();
        char[] buf = new char[4096];
        int p = 0;
        while ((p = reader.read(buf)) >= 0) {
            writer.write(buf, 0, p);
        }
        response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));

        String mongoUrl = System.getenv("MONGO_URL");
        String mongoPort = System.getenv("MONGO_PORT");
        String mongoDbName = System.getenv("MONGO_DB_NAME");

        MongoDBHandler mongo = new MongoDBHandler(mongoUrl, new Integer(mongoPort), mongoDbName, "saffron_runs");
        FindIterable<org.bson.Document> docs = mongo.getCorpus(saffronDatasetName);
        FindIterable<Document> run = mongo.getRun(saffronDatasetName);
        JSONObject configObj = new JSONObject();
        for (Document doc : run) {
            configObj = new JSONObject(doc.toJson());
        }
        String confJson = (String) configObj.get("config");
        JSONObject config = new JSONObject(confJson);
        JSONObject termExtractionConfig = (JSONObject) config.get("termExtraction");
        JSONObject authorTopicConfig = (JSONObject) config.get("authorTopic");
        JSONObject authorSimConfig = (JSONObject) config.get("authorSim");
        JSONObject topicSimConfig = (JSONObject) config.get("topicSim");
        JSONObject taxonomyConfig = (JSONObject) config.get("taxonomy");
        final Configuration newConfig = new Configuration();
        TermExtractionConfiguration terms =
                new ObjectMapper().readValue(termExtractionConfig.toString(), TermExtractionConfiguration.class);
        AuthorTopicConfiguration authorTopic =
                new ObjectMapper().readValue(authorTopicConfig.toString(), AuthorTopicConfiguration.class);
        AuthorSimilarityConfiguration authorSimilarityConfiguration =
                new ObjectMapper().readValue(authorSimConfig.toString(), AuthorSimilarityConfiguration.class);
        TopicSimilarityConfiguration topicSimilarityConfiguration =
                new ObjectMapper().readValue(topicSimConfig.toString(), TopicSimilarityConfiguration.class);
        TaxonomyExtractionConfiguration taxonomyExtractionConfiguration =
                new ObjectMapper().readValue(taxonomyConfig.toString(), TaxonomyExtractionConfiguration.class);
        newConfig.authorSim = authorSimilarityConfiguration;
        newConfig.authorTopic = authorTopic;
        newConfig.taxonomy = taxonomyExtractionConfiguration;
        newConfig.termExtraction = terms;
        newConfig.topicSim = topicSimilarityConfiguration;
  
        List<org.insightcentre.nlp.saffron.data.Document> finalList = new ArrayList<>();
        final IndexedCorpus other = new IndexedCorpus(finalList, new SaffronPath(""));
        for (Document doc : docs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            JSONArray docList = (JSONArray) jsonObj.get("documents");
            for (int i = 0; i < docList.length(); i++) {
                JSONObject obj = (JSONObject) docList.get(i);
                List<Author> authors = new ArrayList<>();
                JSONArray authorList = (JSONArray) obj.get("authors");
                HashMap<String, String> result
                        = new ObjectMapper().readValue(obj.get("metadata").toString(), HashMap.class);
                for (int j = 0; j < authorList.length(); j++) {
                    authors.add((Author) authorList.get(j));
                }
                org.insightcentre.nlp.saffron.data.Document docCorp
                        = new org.insightcentre.nlp.saffron.data.Document(
                                new SaffronPath(""),
                                obj.getString("id"),
                                new URL("http://" + mongoUrl + "/" + mongoPort),
                                obj.getString("name"),
                                obj.getString("mime_type"),
                                authors,
                                result,
                                obj.get("metadata").toString());
                other.addDocument(docCorp);
            }
        }
        corpus = other;

        new Thread(new Runnable() {

            @Override
            public void run() {
                _status.stage = 1;
                try {
                    execute(corpus, newConfig, data.get(saffronDatasetName), saffronDatasetName, false);
                } catch (IOException x) {
                    Status _status = statuses.get(saffronDatasetName);
                    _status.fail(x.getMessage(), x);
                }
            }
        }).start();
    }

    private boolean doAdvancedExecute(HttpServletRequest hsr, final String saffronDatasetName, HttpServletResponse response, Request baseRequest) throws IOException {
        BufferedReader r = hsr.getReader();
        StringBuilder sb = new StringBuilder();
        final Configuration newConfig;
        try {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
            newConfig = new ObjectMapper().readValue(sb.toString(), Configuration.class);
        } catch (Exception x) {
            x.printStackTrace();
            return true;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    execute(corpus, newConfig, data.get(saffronDatasetName), saffronDatasetName, true);
                } catch (IOException x) {
                    Status _status = statuses.get(saffronDatasetName);
                    _status.fail(x.getMessage(), x);
                }
            }
        }).start();
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        return false;
    }

    public void doExecute(String name, HttpServletResponse response, Request baseRequest, HttpServletRequest hsr) throws IOException {
        if (corpus != null && statuses.containsKey(name) && statuses.get(name).advanced) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            String page = FileUtils.readFileToString(new File("static/advanced.html"));
            page = page.replace("{{config}}", new ObjectMapper().writeValueAsString(defaultConfig));
            page = page.replace("{{name}}", hsr.getParameter("name"));
            response.getWriter().print(page);
        } else {
            final String saffronDatasetName = hsr.getParameter("name");
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            FileReader reader = new FileReader(new File("static/executing.html"));
            Writer writer = new StringWriter();
            char[] buf = new char[4096];
            int i = 0;
            while ((i = reader.read(buf)) >= 0) {
                writer.write(buf, 0, i);
            }
            response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
        }
    }

    public void startWithZip(final File tmpFile, final boolean advanced, final String saffronDatasetName) {
        final Status _status = makeStatus();
        _status.name = saffronDatasetName;
        statuses.put(saffronDatasetName, _status);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _status.stage = 1;
                _status.setStatusMessage("Loading ZIP " + tmpFile.getPath());
                try {
                    final Corpus corpus;
                    if (tmpFile.getName().endsWith(".tgz") || tmpFile.getName().endsWith(".tar.gz")) {
                        corpus = CorpusTools.fromTarball(tmpFile, new File(new File(parentDirectory, saffronDatasetName), "docs"));
                    } else {
                        corpus = CorpusTools.fromZIP(tmpFile);
                    }
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        _status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig, data.get(saffronDatasetName), saffronDatasetName, true);
                    }
                } catch (Throwable x) {
                    _status.fail(x.getMessage(), x);
                }
                try {
                    _status.close();
                } catch (IOException x) {
                }
            }
        }).start();
    }

    private Status makeStatus() {
        try {
            if (logFile != null && !logFile.exists()) {
                PrintWriter out = new PrintWriter(logFile);
                out.close();
            }
            return new Status(logFile == null ? null : new PrintWriter(new FileWriter(logFile, true)));
        } catch (IOException x) {
            System.err.println("Could not create logging file: " + x.getMessage());
            return new Status(null);
        }
    }

    public void startWithCrawl(final String url, final int maxPages, final boolean domain,
            final boolean advanced, final String saffronDatasetName) {
        final Status _status = makeStatus();
        _status.name = saffronDatasetName;
        statuses.put(saffronDatasetName, _status);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _status.stage = 1;
                _status.setStatusMessage("Crawling " + url);
                try {
                    URL url2 = new URL(url);
                    File f = Files.createTempDir();
                    String crawlStorageFolder = f.getAbsolutePath();
                    Corpus corpus = SaffronCrawler.crawl(crawlStorageFolder, new File(parentDirectory, saffronDatasetName),
                            null, maxPages, domain ? "\\w+://\\Q" + url2.getHost() + "\\E.*" : ".*",
                            url, 7);
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        _status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig, data.get(saffronDatasetName), saffronDatasetName, true);
                    }
                } catch (Exception x) {
                    _status.fail(x.getMessage(), x);
                }
                try {
                    _status.close();
                } catch (IOException x) {
                }
            }
        }).start();
    }

    public void startWithJson(final File tmpFile, final boolean advanced, final String saffronDatasetName) {
        final Status _status = makeStatus();
        _status.name = saffronDatasetName;
        statuses.put(saffronDatasetName, _status);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _status.stage = 1;
                _status.setStatusMessage("Reading JSON: " + tmpFile.getPath());
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Corpus corpus = CorpusTools.fromJson(tmpFile);
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        _status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig, data.get(saffronDatasetName), saffronDatasetName, true);
                    }
                } catch (Exception x) {
                    _status.fail(x.getMessage(), x);
                }
                try {
                    _status.close();
                } catch (IOException x) {
                }
            }
        }).start();
    }

    private void scaleThreads(Configuration config) {
        long heapSize = Runtime.getRuntime().maxMemory();
        if ((long) config.termExtraction.numThreads * 1024 * 1024 * 400 > heapSize) {
            int numThreads = (int) Math.ceil((double) heapSize / 1024 / 1024 / 400);
            System.err.println(String.format("System memory %d MB", heapSize / 1024 / 1024));
            System.err.println(String.format("Insufficient memory for %d threads, reducing to %d", config.termExtraction.numThreads, numThreads));
            System.err.println("Try setting the -Xmx flag to the Java Runtime to improve performance");
            config.termExtraction.numThreads = numThreads;
        }
    }

    void execute(Corpus corpus, Configuration config, SaffronData data, String saffronDatasetName, Boolean isInitialRun) throws IOException {
        Status _status = statuses.get(saffronDatasetName);
        _status.advanced = false;
        BlackWhiteList bwList = extractBlackWhiteList(saffronDatasetName);
        if (bwList == null) {
            bwList = new BlackWhiteList();

        }
        scaleThreads(config);

        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();

        final File datasetFolder = new File(parentDirectory, saffronDatasetName);
        if (!datasetFolder.exists()) {
            if (!datasetFolder.mkdirs()) {
                System.err.println("Could not make dataset folder, this run is likely to fail!");
            }
        }
        ow.writeValue(new File(datasetFolder, "config.json"), config);

        _status.stage++;
        _status.setStatusMessage("Indexing Corpus");
        final File indexFile = new File(new File(parentDirectory, saffronDatasetName), "index");
        DocumentSearcher searcher = DocumentSearcherFactory.index(corpus, indexFile, _status, isInitialRun);

        _status.stage++;
        _status.setStatusMessage("Initializing topic extractor");
        //TopicExtraction extractor = new TopicExtraction(config.termExtraction);
        TermExtraction extractor = new TermExtraction(config.termExtraction);
        _status.setStatusMessage("Extracting Topics");
        TermExtraction.Result res = extractor.extractTopics(searcher, bwList.termWhiteList, bwList.termBlackList, _status);
        //res.normalize();

        _status.setStatusMessage("Writing extracted topics");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "topics-extracted.json"), res.topics);

        _status.setStatusMessage("Writing document topic correspondence");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "doc-topics.json"), res.docTopics);

        data.setDocTopics(res.docTopics);

        _status.stage++;
        _status.setStatusMessage("Extracting authors from corpus");
        Set<Author> authors = Consolidate.extractAuthors(searcher, _status);

        _status.setStatusMessage("Consolidating author names");
        Map<Author, Set<Author>> consolidation = ConsolidateAuthors.consolidate(authors, _status);

        _status.setStatusMessage("Applying consolidation to corpus");
        applyConsolidation(searcher, consolidation, _status);
        data.setCorpus(searcher);

        _status.stage++;
        _status.setStatusMessage("Linking to DBpedia");
        // TODO: Even the LinkToDBpedia executable literally does nothing!
        List<Topic> topics = new ArrayList<>(res.topics);
        data.setTopics(res.topics);

        _status.setStatusMessage("Saving linked topics");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "topics.json"), topics);

        _status.stage++;
        _status.setStatusMessage("Connecting authors to topics");
        ConnectAuthorTopic cr = new ConnectAuthorTopic(config.authorTopic);
        Collection<AuthorTopic> authorTopics = cr.connectResearchers(topics, res.docTopics, searcher.getDocuments(), _status);

        _status.setStatusMessage("Saving author connections");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "author-topics.json"), authorTopics);

        data.setAuthorTopics(authorTopics);

        _status.stage++;
        _status.setStatusMessage("Connecting topics");
        TopicSimilarity ts = new TopicSimilarity(config.topicSim);
        final List<TopicTopic> topicSimilarity = ts.topicSimilarity(res.docTopics, _status);

        _status.setStatusMessage("Saving topic connections");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "topic-sim.json"), topicSimilarity);

        data.setTopicSim(topicSimilarity);

        _status.stage++;
        _status.setStatusMessage("Connecting authors to authors");
        AuthorSimilarity as = new AuthorSimilarity(config.authorSim);
        final List<AuthorAuthor> authorSim = as.authorSimilarity(authorTopics, _status);

        _status.setStatusMessage("Saving author connections");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "author-sim.json"), authorSim);

        data.setAuthorSim(authorSim);

        _status.stage++;
        _status.setStatusMessage("Building topic map");
        Map<String, Topic> topicMap = loadMap(topics, mapper, _status);

        //Taxonomy graph = extractTaxonomy(res.docTopics, topicMap);
        _status.setStatusMessage("Reading model");
        if (config.taxonomy.modelFile == null) {
            config.taxonomy.modelFile = new SaffronPath("${saffron.home}/models/default.json");
        }
        Model model = mapper.readValue(config.taxonomy.modelFile.toFile(), Model.class);

        SupervisedTaxo supTaxo = new SupervisedTaxo(res.docTopics, topicMap, model);
        _status.setStatusMessage("Building taxonomy");
        TaxonomySearch search = TaxonomySearch.create(config.taxonomy.search, supTaxo, topicMap.keySet());
        final Taxonomy graph = search.extractTaxonomyWithBlackWhiteList(topicMap, bwList.taxoWhiteList, bwList.taxoBlackList);
        // Insert HEAD_TOPIC into top of solution
        List<Taxonomy> newChildren = new ArrayList<>();
        newChildren.add(graph.deepCopy());
        Taxonomy topRootGraph = new Taxonomy("HEAD_TOPIC", 0.0, 0.0, "", "", newChildren, org.insightcentre.nlp.saffron.data.Status.none);
        _status.setStatusMessage("Saving taxonomy");

        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "taxonomy.json"), topRootGraph);
        data.setTaxonomy(graph);

        try {

            String mongoUrl = System.getenv("MONGO_URL");
            String mongoPort = System.getenv("MONGO_PORT");
            String mongoDbName = System.getenv("MONGO_DB_NAME");

            MongoDBHandler mongo = new MongoDBHandler(mongoUrl, new Integer(mongoPort), mongoDbName, "saffron_runs");
            mongo.deleteRun(saffronDatasetName);
            mongo.addRun(saffronDatasetName, new Date(), config);
            mongo.addDocumentTopicCorrespondence(saffronDatasetName, new Date(), res.docTopics);
            mongo.addTopics(saffronDatasetName, new Date(), topics);
            mongo.addTopicExtraction(saffronDatasetName, new Date(), res.topics);
            mongo.addAuthorTopics(saffronDatasetName, new Date(), topics);
            mongo.addAuthorSimilarity(saffronDatasetName, new Date(), authorSim);
            mongo.addTopicsSimilarity(saffronDatasetName, new Date(), topicSimilarity);
            mongo.addTaxonomy(saffronDatasetName, new Date(), topRootGraph);
            mongo.addCorpus(saffronDatasetName, new Date(), corpus);

        } catch (MongoException ex) {
            System.out.println("MongoDB not available - starting execution in local mode");
        }

        _status.setStatusMessage("Done");
        _status.completed = true;
    }

    public BlackWhiteList extractBlackWhiteList(String datasetName) {
        String mongoUrl = System.getenv("MONGO_URL");
        String mongoPort = System.getenv("MONGO_PORT");
        String mongoDbName = System.getenv("MONGO_DB_NAME");

        MongoDBHandler mongo = new MongoDBHandler(mongoUrl, new Integer(mongoPort), mongoDbName, "saffron_runs");

        if(!mongo.getTopics(datasetName).iterator().hasNext()) {
            return new BlackWhiteList();
        }
        else {
            return BlackWhiteList.from(mongo.getTopics(datasetName), mongo.getTaxonomy(datasetName));

        }
    }

    public class Status implements SaffronListener, Closeable {

        public int stage = 0;
        public boolean failed = false;
        public boolean completed = false;
        public boolean advanced = false;
        private String statusMessage2 = "";
        public String name;
        private final PrintWriter out;

        public Status(PrintWriter out) {
            this.out = out;
        }

        public String getStatusMessage() {
            return statusMessage2;
        }

        public void setStatusMessage(String statusMessage) {
            System.err.printf("[STAGE %d] %s\n", stage, statusMessage);
            if(out != null) out.printf("[STAGE %d] %s\n", stage, statusMessage);
            if(out != null) out.flush();
            this.statusMessage2 = statusMessage;
        }

        @Override
        public void fail(String message, Throwable cause) {
            this.failed = true;
            setStatusMessage("Failed: " + message);
            data.remove(name);
            cause.printStackTrace();
            if(out != null) cause.printStackTrace(out);
            if(out != null) out.flush();
        }

        @Override
        public void log(String message) {
            System.err.println(message);
            if(out != null) out.println(message);
            if(out != null) out.flush();
        }

        @Override
        public void endTick() {
            System.err.println();
            if(out != null) out.println();
            if(out != null) out.flush();
        }

        @Override
        public void tick() {
            System.err.print(".");
            if(out != null) out.print(".");
            if(out != null) out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }

}
