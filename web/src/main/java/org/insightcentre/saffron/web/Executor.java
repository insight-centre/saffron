package org.insightcentre.saffron.web;

import static org.insightcentre.nlp.saffron.authors.Consolidate.applyConsolidation;
import static org.insightcentre.nlp.saffron.taxonomy.supervised.Main.loadMap;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.flogger.FluentLogger;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.authors.Consolidate;
import org.insightcentre.nlp.saffron.authors.ConsolidateAuthors;
import org.insightcentre.nlp.saffron.authors.connect.ConnectAuthorTerm;
import org.insightcentre.nlp.saffron.authors.sim.AuthorSimilarity;
import org.insightcentre.nlp.saffron.concept.consolidation.AlgorithmFactory;
import org.insightcentre.nlp.saffron.concept.consolidation.ConceptConsolidation;
import org.insightcentre.nlp.saffron.config.AuthorSimilarityConfiguration;
import org.insightcentre.nlp.saffron.config.AuthorTermConfiguration;
import org.insightcentre.nlp.saffron.config.ConceptConsolidationConfiguration;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TermSimilarityConfiguration;
import org.insightcentre.nlp.saffron.crawler.SaffronCrawler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Concept;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Model;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.VirtualRootTaxonomy;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.documentindex.IndexedCorpus;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxonomySearch;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;
import org.insightcentre.nlp.saffron.term.TermExtraction;
import org.insightcentre.nlp.saffron.topic.tfidf.TFIDF;
import org.insightcentre.nlp.saffron.topic.topicsim.TermSimilarity;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Files;
import com.mongodb.client.FindIterable;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Executor extends AbstractHandler {

    private final SaffronDataSource data;
    private final File parentDirectory;
    private final Map<String, Status> statuses;
    private Corpus corpus;
    private Configuration defaultConfig;
    private final File logFile;
    static String storeCopy = System.getenv("STORE_LOCAL_COPY");
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public Executor(SaffronDataSource data, File directory, File logFile) {
        this.data = data;
        this.parentDirectory = directory;
        this.statuses = new HashMap<>();
        try {
            this.defaultConfig = new ObjectMapper().readValue(new SaffronPath("${saffron.home}/models/config.json").toFile(), Configuration.class);
        } catch (IOException x) {
            this.defaultConfig = new Configuration();
            logger.atInfo().log("Could not load config.json in models folder... using default configuration (" + x.getMessage() + ")");

        }
        this.logFile = logFile;

    }

    /**
     * Initialize a new Saffron Dataset
     *
     * @param name The name
     * @return True if a new dataset was created
     */
    public boolean newDataSet(String name) throws IOException {
        if (data.containsKey(name)) {
            return false;
        } else {
            data.addRun(name, new Date(), this.defaultConfig);
            return true;
        }
    }

    public boolean isExecuting(String name) {
        return statuses.containsKey(name) && statuses.get(name).stage > 0 && !statuses.get(name).completed;
    }

    public File getParentDirectory() {
        return this.parentDirectory;
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
                doAdvancedExecute(hsr, saffronDatasetName, response, baseRequest);

            } else if (target.startsWith("/api/v1/run/rerun")) {
                final String saffronDatasetName = target.substring("/api/v1/run/rerun/".length());
                if(saffronDatasetName != null && data.containsKey(saffronDatasetName)){
                    doRerun(saffronDatasetName, response, baseRequest);
                }
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
        if (mongoUrl == null) {
            mongoUrl = "localhost";
        }
        String mongoPort = System.getenv("MONGO_PORT");
        if (mongoPort == null) {
            mongoPort = "27017";
        }

        MongoDBHandler mongo = new MongoDBHandler();
        FindIterable<org.bson.Document> docs = mongo.getCorpus(saffronDatasetName);
        String run = mongo.getRun(saffronDatasetName);
        JSONObject configObj = new JSONObject(run);
        String confJson = (String) configObj.get("config");
        JSONObject config = new JSONObject(confJson);
        JSONObject termExtractionConfig = (JSONObject) config.get("termExtraction");
        JSONObject authorTermConfig = (JSONObject) config.get("authorTerm");
        JSONObject authorSimConfig = (JSONObject) config.get("authorSim");
        JSONObject termSimConfig = (JSONObject) config.get("termSim");
        JSONObject taxonomyConfig = (JSONObject) config.get("taxonomy");
        final Configuration newConfig = new Configuration();
        TermExtractionConfiguration terms
                = new ObjectMapper().readValue(termExtractionConfig.toString(), TermExtractionConfiguration.class);
        AuthorTermConfiguration authorTerm
                = new ObjectMapper().readValue(authorTermConfig.toString(), AuthorTermConfiguration.class);
        AuthorSimilarityConfiguration authorSimilarityConfiguration
                = new ObjectMapper().readValue(authorSimConfig.toString(), AuthorSimilarityConfiguration.class);
        TermSimilarityConfiguration termSimilarityConfiguration
                = new ObjectMapper().readValue(termSimConfig.toString(), TermSimilarityConfiguration.class);
        TaxonomyExtractionConfiguration taxonomyExtractionConfiguration
                = new ObjectMapper().readValue(taxonomyConfig.toString(), TaxonomyExtractionConfiguration.class);
        ConceptConsolidationConfiguration conceptConsolidationConfiguration
        		= new ObjectMapper().readValue(taxonomyConfig.toString(), ConceptConsolidationConfiguration.class);
        newConfig.authorSim = authorSimilarityConfiguration;
        newConfig.authorTerm = authorTerm;
        newConfig.taxonomy = taxonomyExtractionConfiguration;
        newConfig.termExtraction = terms;
        newConfig.termSim = termSimilarityConfiguration;
        newConfig.conceptConsolidation = conceptConsolidationConfiguration;

        List<org.insightcentre.nlp.saffron.data.Document> finalList = new ArrayList<>();
        final IndexedCorpus other = new IndexedCorpus(finalList, new SaffronPath(""));
        for (Document doc : docs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            JSONArray docList;
            try {
                docList = (JSONArray) jsonObj.get("documents");
            } catch (Exception e) {
                docList = new JSONArray();
            }

            for (int i = 0; i < docList.length(); i++) {
                JSONObject obj = (JSONObject) docList.get(i);

                JSONArray authorList = (JSONArray) obj.get("authors");
                HashMap<String, String> result
                        = new ObjectMapper().readValue(obj.get("metadata").toString(), HashMap.class);
                List<Author> authors = Arrays.asList(
                        new ObjectMapper().readValue(obj.get("authors").toString(), Author[].class));

                org.insightcentre.nlp.saffron.data.Document docCorp
                        = new org.insightcentre.nlp.saffron.data.Document(
                        new SaffronPath(""),
                        obj.getString("id"),
                        new URL("http://" + mongoUrl + "/" + mongoPort),
                        obj.getString("name"),
                        obj.getString("mime_type"),
                        authors,
                        result,
                        obj.get("metadata").toString(),
                        obj.has("date") ? org.insightcentre.nlp.saffron.data.Document.parseDate(obj.get("date").toString()) : null);
                other.addDocument(docCorp);
            }
        }
        corpus = other;

        new Thread(new Runnable() {

            @Override
            @SuppressWarnings("UseSpecificCatch")
            public void run() {
                _status.stage = 1;
                try {
                    execute(corpus, newConfig, data, saffronDatasetName, false);
                } catch (Throwable x) {
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
        // Clear the 'advanced' status so the system switches to the spinner
        statuses.get(saffronDatasetName).advanced = false;
        new Thread(new Runnable() {
            @Override
            @SuppressWarnings("UseSpecificCatch")
            public void run() {
                try {
                    execute(corpus, newConfig, data, saffronDatasetName, true);
                } catch (Exception x) {
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
                _status.setStageStart("Loading corpus" + tmpFile.getPath(), saffronDatasetName);
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
                        execute(corpus, defaultConfig, data, saffronDatasetName, true);
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
                _status.setStageStart("Loading corpus" + url, saffronDatasetName);
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
                        execute(corpus, defaultConfig, data, saffronDatasetName, true);
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
                _status.setStageStart("Loading corpus" + tmpFile.getPath(), saffronDatasetName);
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Corpus corpus = CorpusTools.fromJson(tmpFile);
                    List<org.insightcentre.nlp.saffron.data.Document> newDocs = new ArrayList<>();
                    if (corpus.getDocuments() != null) {
                        for (org.insightcentre.nlp.saffron.data.Document doc : corpus.getDocuments()) {
                            if (doc.file != null) {
                                FileReader reader = new FileReader(doc.file.toFile());
                                Writer writer = new StringWriter();
                                char[] buf = new char[4096];
                                int i = 0;
                                while ((i = reader.read(buf)) >= 0) {
                                    writer.write(buf, 0, i);
                                }
                                org.insightcentre.nlp.saffron.data.Document newDoc
                                        = new org.insightcentre.nlp.saffron.data.Document(doc.file,
                                            doc.id, doc.url, doc.name, doc.mimeType,
                                        doc.authors, doc.metadata, writer.toString(), doc.date);
                                newDocs.add(newDoc);
                            }
                        }

                        if (newDocs.size() > 0) {
                            corpus = CorpusTools.fromJsonFiles(tmpFile);
                        }
                    }
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        _status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig, data, saffronDatasetName, true);
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

    void execute(Corpus corpus, Configuration config, SaffronDataSource data, String saffronDatasetName, Boolean isInitialRun) throws IOException {
        BlackWhiteList bwList = extractBlackWhiteList(saffronDatasetName);
        if (bwList == null) {
            bwList = new BlackWhiteList();

        }
        data.deleteRun(saffronDatasetName);
        data.addRun(saffronDatasetName, new Date(), config);
        Status _status = statuses.get(saffronDatasetName);
        _status.advanced = false;
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
        _status.setStageComplete("Loading corpus", saffronDatasetName);
        _status.stage++;

        _status.setStageStart("Indexing Corpus", saffronDatasetName);
        final File indexFile = new File(new File(parentDirectory, saffronDatasetName), "index");
        DocumentSearcher searcher = DocumentSearcherFactory.index(corpus, indexFile, _status, isInitialRun);
        _status.setStageComplete("Indexing Corpus", saffronDatasetName);

        _status.stage++;

        _status.setStageStart("Extracting Terms", saffronDatasetName);
        TermExtraction extractor = new TermExtraction(config.termExtraction);
        TermExtraction.Result res = extractor.extractTerms(searcher, bwList.termWhiteList, bwList.termBlackList, _status);
        List<Term> terms = new ArrayList<>(res.terms);
        data.setTerms(saffronDatasetName, terms);

        //_status.setStageStart("Writing extracted terms", saffronDatasetName);
        //if (storeCopy.equals("true"))
        //    ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "terms-extracted.json"), res.topics);

        _status.setStageStart("Writing Document-Term Correspondence", saffronDatasetName);
        if (storeCopy.equals("true"))
            ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "doc-terms.json"), res.docTerms);
        data.setDocTerms(saffronDatasetName, res.docTerms);
        _status.setStageComplete("Writing Document-Term Correspondence", saffronDatasetName);
        _status.setStageComplete("Extracting Terms", saffronDatasetName);

        _status.stage++;
        _status.setStageStart("Consolidating concepts", saffronDatasetName);
        ConceptConsolidation conceptConsolidation = AlgorithmFactory.create(config.conceptConsolidation);
        List<Concept> concepts = conceptConsolidation.consolidate(terms);
        data.addConcepts(saffronDatasetName, concepts);
        _status.setStageComplete("Consolidating concepts", saffronDatasetName);
        
        _status.stage++;

        _status.setStageStart("Extracting authors from corpus", saffronDatasetName);
        Set<Author> authors = Consolidate.extractAuthors(searcher, _status);
        Map<Author, Set<Author>> consolidation = ConsolidateAuthors.consolidate(authors, _status);
        applyConsolidation(searcher, consolidation, _status);
        data.setCorpus(saffronDatasetName, corpus);
        _status.setStageComplete("Extracting authors from corpus", saffronDatasetName);

        _status.stage++;

        _status.setStageStart("Saving linked terms", saffronDatasetName);
        if (storeCopy.equals("true"))
            ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "terms.json"), terms);
        _status.setStageComplete("Saving linked terms", saffronDatasetName);

        _status.stage++;

        _status.setStageStart("Connecting authors to terms", saffronDatasetName);
        TFIDF.addTfidf(res.docTerms);
        ConnectAuthorTerm cr = new ConnectAuthorTerm(config.authorTerm);
        Collection<AuthorTerm> authorTerms = cr.connectResearchers(terms, res.docTerms, searcher.getDocuments(), _status);
        if (storeCopy.equals("true"))
            ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "author-terms.json"), authorTerms);
        data.setAuthorTerms(saffronDatasetName, authorTerms);
        _status.setStageComplete("Connecting authors to terms", saffronDatasetName);

        _status.stage++;

        _status.setStageStart("Connecting terms", saffronDatasetName);
        TermSimilarity ts = new TermSimilarity(config.termSim);
        final List<TermTerm> termSimilarity = ts.termSimilarity(res.docTerms, _status);
        if (storeCopy.equals("true"))
            ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "term-sim.json"), termSimilarity);
        data.setTermSim(saffronDatasetName, termSimilarity);
        _status.setStageComplete("Connecting terms", saffronDatasetName);

        _status.stage++;

        _status.setStageStart("Connecting authors to authors", saffronDatasetName);
        AuthorSimilarity as = new AuthorSimilarity(config.authorSim);
        final List<AuthorAuthor> authorSim = as.authorSimilarity(authorTerms, _status);
        if (storeCopy.equals("true"))
            ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "author-sim.json"), authorSim);
        data.setAuthorSim(saffronDatasetName, authorSim);
        _status.setStageComplete("Connecting authors to authors", saffronDatasetName);

        _status.stage++;

        _status.setStageStart("Building term map and taxonomy", saffronDatasetName);
        Map<String, Term> termMap = loadMap(terms, mapper, _status);
        if (config.taxonomy.modelFile == null) {
            config.taxonomy.modelFile = new SaffronPath("${saffron.home}/models/default.json");
        }
        Model model = mapper.readValue(config.taxonomy.modelFile.toFile(), Model.class);
        SupervisedTaxo supTaxo = new SupervisedTaxo(res.docTerms, termMap, model);
        TaxonomySearch search = TaxonomySearch.create(config.taxonomy.search, supTaxo, termMap.keySet());
        final Taxonomy graph = search.extractTaxonomyWithBlackWhiteList(termMap, bwList.taxoWhiteList, bwList.taxoBlackList);
        Taxonomy topRootGraph = new VirtualRootTaxonomy(graph);
        if (storeCopy.equals("true"))
            ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "taxonomy.json"), topRootGraph);
        data.setTaxonomy(saffronDatasetName, topRootGraph);
        _status.setStageComplete("Building term map and taxonomy", saffronDatasetName);
        _status.completed = true;
    }

    public BlackWhiteList extractBlackWhiteList(String datasetName) {

        MongoDBHandler mongo = new MongoDBHandler();

        if (!mongo.getTerms(datasetName).iterator().hasNext()) {
            return new BlackWhiteList();
        } else {
            return BlackWhiteList.from(mongo.getTerms(datasetName), mongo.getTaxonomy(datasetName));

        }
    }

    public class Status implements SaffronListener, Closeable {

        public int stage = 0;
        public boolean failed = false;
        public boolean completed = false;
        public boolean advanced = false;
        public boolean warning = false;
        public String name;
        public String statusMessage;
        private final PrintWriter out;

        public Status(PrintWriter out) {
            this.out = out;
        }

        public void setStatusMessage(String statusMessage) {
            logger.atInfo().log("[STAGE %d] %s\n", stage, statusMessage);
            if (out != null) {
                out.printf("[STAGE %d] %s\n", stage, statusMessage);
            }
            if (out != null) {
                out.flush();
            }
        }

        public void setErrorMessage(String errorMessage) {
            logger.atSevere().log("[STAGE %d] %s\n", stage, errorMessage);
            this.statusMessage = errorMessage;
        }

        public void setWarningMessage(String warningMessage) {
            logger.atWarning().log("[STAGE %d] %s\n", stage, warningMessage);
            this.statusMessage = warningMessage;
        }

        @Override
        public void setStageStart(String statusMessage, String taxonomyId) {
            logger.atInfo().log("[STAGE %d] %s\n", stage, statusMessage);
            String run = data.getRun(taxonomyId);
            JSONObject runJson = new JSONObject(run);
            data.updateRun(taxonomyId, statusMessage, runJson, "running");
            if (out != null) {
                out.printf("[STAGE %d] %s\n", stage, statusMessage);
            }
            if (out != null) {
                out.flush();
            }
            this.statusMessage = statusMessage;
        }

        @Override
        public void setStageComplete(String statusMessage, String taxonomyId) {
            String run = data.getRun(taxonomyId);
            JSONObject runJson = new JSONObject(run);
            data.updateRun(taxonomyId, statusMessage, runJson, "completed");
        }

        @Override
        public void warning(String message, Throwable cause) {
            this.warning = true;
            setWarningMessage("Warning: " + message);
            cause.printStackTrace();
            if (out != null) {
                cause.printStackTrace(out);
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void fail(String message, Throwable cause) {
            this.failed = true;
            if (cause != null && cause instanceof com.mongodb.MongoTimeoutException){
                setErrorMessage("Failed: Cannot connect to a MongoDB instance. " +
                        "Please configure Saffron to connect to a running MongoDB instance and try again.");
            } else {
                setErrorMessage("Failed: " + message);
            }
            data.remove(name);
            if(cause != null)
                cause.printStackTrace();
            if (out != null) {
                cause.printStackTrace(out);
            }
            if (out != null) {
                out.flush();
            }
            if(cause != null)
                logger.atSevere().log("Failed due to " + cause.getClass().getName() + ": " + message);
            else 
                logger.atSevere().log("Failed: " + message);
        }

        @Override
        public void log(String message) {
            logger.atInfo().log("[STAGE %d] %s\n", stage, message);
            if (out != null) {
                out.println(message);
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void endTick() {
            logger.atInfo().log("[STAGE %d] %s\n", stage, "");
            if (out != null) {
                out.println();
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void tick() {
            logger.atInfo().log("[STAGE %d] %s\n", stage, ".");
            if (out != null) {
                out.print(".");
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if(out != null)
                out.close();
        }
    }

}
