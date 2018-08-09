package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.authors.Consolidate;
import static org.insightcentre.nlp.saffron.authors.Consolidate.applyConsolidation;
import org.insightcentre.nlp.saffron.authors.ConsolidateAuthors;
import org.insightcentre.nlp.saffron.authors.connect.ConnectAuthorTopic;
import org.insightcentre.nlp.saffron.authors.sim.AuthorSimilarity;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.crawler.SaffronCrawler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.taxonomy.supervised.GreedyTaxoExtract;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MSTTaxoExtract;
import static org.insightcentre.nlp.saffron.taxonomy.supervised.Main.loadMap;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;
import static org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration.Mode.greedy;
import static org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration.Mode.greedyTrans;
import static org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration.Mode.headAndBag;
import org.insightcentre.nlp.saffron.data.Model;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.taxonomy.supervised.HeadAndBag;
import org.insightcentre.nlp.saffron.taxonomy.supervised.TransTaxoExtract;
import org.insightcentre.nlp.saffron.term.TermExtraction;
import org.insightcentre.nlp.saffron.topic.topicsim.TopicSimilarity;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Executor extends AbstractHandler {

    private final Map<String, SaffronData> data;
    private final File parentDirectory;
    private final Map<String, Status> statuses;
    private Corpus corpus;
    private Configuration defaultConfig;
    private Configuration config;

    public Executor(Map<String, SaffronData> data, File directory) {
        this.data = data;
        this.parentDirectory = directory;
        this.statuses = new HashMap<>();
        try {
            this.defaultConfig = new ObjectMapper().readValue(new SaffronPath("${saffron.home}/models/config.json").toFile(), Configuration.class);
        } catch (IOException x) {
            this.defaultConfig = new Configuration();
            System.err.println("Could not load config.json in models folder... using default configuration (" + x.getMessage() + ")");
        }

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
                if (corpus != null && config == null && statuses.containsKey(name) && statuses.get(name).advanced) {
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
            } else if (corpus != null && config == null && (target.startsWith("/execute/advanced/"))) {
                final String saffronDatasetName = target.substring("/execute/advanced/".length());
                BufferedReader r = hsr.getReader();
                StringBuilder sb = new StringBuilder();
                try {
                    String line;
                    while ((line = r.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    this.config = new ObjectMapper().readValue(sb.toString(), Configuration.class);
                } catch (Exception x) {
                    x.printStackTrace();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            execute(corpus, config, data.get(saffronDatasetName), saffronDatasetName);
                        } catch (IOException x) {
                            Status _status = statuses.get(saffronDatasetName);
                            _status.failed = true;
                            _status.setStatusMessage("Failed: " + x.getMessage());
                            x.printStackTrace();
                        }
                    }
                }).start();
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);

            }
            if ("/execute/status".equals(target)) {
                String saffronDatasetName = hsr.getParameter("name");
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getWriter(), statuses.get(saffronDatasetName));
            }
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    void startWithZip(final File tmpFile, final boolean advanced, final String saffronDatasetName) {
        final Status _status = new Status();
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
                        corpus = CorpusTools.fromZIP(tmpFile, new File(new File(parentDirectory, saffronDatasetName), "docs"));
                    }
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        _status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig, data.get(saffronDatasetName), saffronDatasetName);
                    }
                } catch (Throwable x) {
                    _status.failed = true;
                    _status.setStatusMessage("Failed: " + x.getMessage());
                    x.printStackTrace();
                }
            }
        }).start();
    }

    void startWithCrawl(final String url, final int maxPages, final boolean domain,
            final boolean advanced, final String saffronDatasetName) {
        final Status _status = new Status();
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
                        execute(corpus, defaultConfig, data.get(saffronDatasetName), saffronDatasetName);
                    }
                } catch (Exception x) {
                    _status.failed = true;
                    _status.setStatusMessage("Failed: " + x.getMessage());
                    x.printStackTrace();
                }
            }
        }).start();
    }

    void startWithJson(final File tmpFile, final boolean advanced, final String saffronDatasetName) {
        final Status _status = new Status();
        statuses.put(saffronDatasetName, _status);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _status.stage = 1;
                _status.setStatusMessage("Reading JSON: " + tmpFile.getPath());
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    IndexedCorpus corpus = mapper.readValue(tmpFile, IndexedCorpus.class);
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        _status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig, data.get(saffronDatasetName), saffronDatasetName);
                    }
                } catch (Exception x) {
                    _status.failed = true;
                    _status.setStatusMessage("Failed: " + x.getMessage());
                    x.printStackTrace();
                }
            }
        }).start();
    }

    void execute(Corpus corpus, Configuration config, SaffronData data, String saffronDatasetName) throws IOException {
        Status _status = statuses.get(saffronDatasetName);
        _status.advanced = false;
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();

        _status.stage++;
        _status.setStatusMessage("Indexing Corpus");
        final File indexFile = new File(new File(parentDirectory, saffronDatasetName), "index");
        DocumentSearcher searcher = DocumentSearcherFactory.loadSearcher(corpus, indexFile, true);
        _status.setStatusMessage("Loading index");
        ArrayList<Document> docs = new ArrayList<>();
        for (Document d : corpus.getDocuments()) {
            docs.add(d);
        }
        IndexedCorpus indexedCorpus = new IndexedCorpus(docs, SaffronPath.fromFile(indexFile));

        _status.stage++;
        _status.setStatusMessage("Initializing topic extractor");
        //TopicExtraction extractor = new TopicExtraction(config.termExtraction);
        TermExtraction extractor = new TermExtraction(config.termExtraction);

        _status.setStatusMessage("Extracting Topics");
        TermExtraction.Result res = extractor.extractTopics(searcher);

        _status.setStatusMessage("Writing extracted topics");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "topics-extracted.json"), res.topics);

        _status.setStatusMessage("Writing document topic correspondence");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "doc-topics.json"), res.docTopics);
        data.setDocTopics(res.docTopics);

        _status.stage++;
        _status.setStatusMessage("Extracting authors from corpus");
        Set<Author> authors = Consolidate.extractAuthors(indexedCorpus);

        _status.setStatusMessage("Consolidating author names");
        Map<Author, Set<Author>> consolidation = ConsolidateAuthors.consolidate(authors);

        _status.setStatusMessage("Applying consoliation to corpus");
        IndexedCorpus corpus2 = applyConsolidation(indexedCorpus, consolidation);

        _status.setStatusMessage("Writing consolidated corpus");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "corpus.json"), corpus2);
        data.setCorpus(corpus2);

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
        Collection<AuthorTopic> authorTopics = cr.connectResearchers(topics, res.docTopics, corpus2.documents);

        _status.setStatusMessage("Saving author connections");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "author-topics.json"), authorTopics);
        data.setAuthorTopics(authorTopics);

        _status.stage++;
        _status.setStatusMessage("Connecting topics");
        TopicSimilarity ts = new TopicSimilarity(config.topicSim);
        final List<TopicTopic> topicSimilarity = ts.topicSimilarity(res.docTopics);

        _status.setStatusMessage("Saving topic connections");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "topic-sim.json"), topicSimilarity);
        data.setTopicSim(topicSimilarity);

        _status.stage++;
        _status.setStatusMessage("Connecting authors to authors");
        AuthorSimilarity as = new AuthorSimilarity(config.authorSim);
        final List<AuthorAuthor> authorSim = as.authorSimilarity(authorTopics);

        _status.setStatusMessage("Saving author connections");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "author-sim.json"), authorSim);
        data.setAuthorSim(authorSim);

        _status.stage++;
        _status.setStatusMessage("Building topic map");
        Map<String, Topic> topicMap = loadMap(topics, mapper);

        //Taxonomy graph = extractTaxonomy(res.docTopics, topicMap);
        _status.setStatusMessage("Reading model");
        if (config.taxonomy.modelFile == null) {
            config.taxonomy.modelFile = new SaffronPath("${saffron.home}/models/default.json");
        }
        Model model = mapper.readValue(config.taxonomy.modelFile.toFile(), Model.class);

        SupervisedTaxo supTaxo = new SupervisedTaxo(res.docTopics, topicMap, model);
        _status.setStatusMessage("Building taxonomy");
        final Taxonomy graph;
        if (topicMap.isEmpty()) {
            graph = new Taxonomy("<EMPTY>", 0, Collections.EMPTY_LIST);
        } else if (config.taxonomy.mode == greedyTrans) {
            TransTaxoExtract taxoExtractor = new TransTaxoExtract(supTaxo, 0.5);
            graph = taxoExtractor.extractTaxonomy(topicMap.keySet());
        } else if (config.taxonomy.mode == greedy) {
            GreedyTaxoExtract taxoExtractor = new GreedyTaxoExtract(supTaxo, config.taxonomy.maxChildren);
            graph = taxoExtractor.extractTaxonomy(res.docTopics, topicMap);
        } else if (config.taxonomy.mode == headAndBag) {
            HeadAndBag taxoExtractor = new HeadAndBag(supTaxo, 0.5);
            graph = taxoExtractor.extractTaxonomy(topicMap.keySet());
        } else {
            MSTTaxoExtract taxoExtractor = new MSTTaxoExtract(supTaxo);
            graph = taxoExtractor.extractTaxonomy(res.docTopics, topicMap);
        }

        _status.setStatusMessage("Saving taxonomy");
        ow.writeValue(new File(new File(parentDirectory, saffronDatasetName), "taxonomy.json"), graph);
        data.setTaxonomy(graph);

        _status.setStatusMessage("Done");
        _status.completed = true;
    }

    public static class Status {

        public int stage = 0;
        public boolean failed = false;
        public boolean completed = false;
        public boolean advanced = false;
        private String statusMessage2 = "";

        public String getStatusMessage() {
            return statusMessage2;
        }

        public void setStatusMessage(String statusMessage) {
            System.err.printf("[STAGE %d] %s\n", stage, statusMessage);
            this.statusMessage2 = statusMessage;
        }

    }

}
