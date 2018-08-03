package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private final SaffronData data;
    private final File directory;
    private final Status status;
    private Corpus corpus;
    private Configuration defaultConfig;
    private Configuration config;

    public Executor(SaffronData data, File directory) {
        this.data = data;
        this.directory = directory;
        this.status = new Status();
        try {
            this.defaultConfig = new ObjectMapper().readValue(new File("../models/config.json"), Configuration.class);
        } catch (IOException x) {
            this.defaultConfig = new Configuration();
            System.err.println("Could not load config.json in models folder... using default configuration");
        }

    }

    public boolean isExecuting() {
        return status.stage > 0 && !status.completed;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest hsr,
            HttpServletResponse response) throws IOException, ServletException {
        try {
            if (isExecuting()) {
                if (corpus != null && config == null && (target == null || "".equals(target) || "/".equals(target))) {
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    String page = FileUtils.readFileToString(new File("static/advanced.html"));
                    page = page.replace("{{config}}", new ObjectMapper().writeValueAsString(defaultConfig));
                    response.getWriter().print(page);
                } else if (corpus != null && config == null && ("/advanced".equals(target))) {
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
                                execute(corpus, config);
                            } catch (IOException x) {
                                status.failed = true;
                                status.setStatusMessage("Failed: " + x.getMessage());
                                x.printStackTrace();
                            }
                        }
                    }).start();
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                } else if (target == null || "".equals(target) || "/".equals(target)) {
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File("static/executing.html"));
                    Writer writer = response.getWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                } else if ("/status".equals(target)) {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(response.getWriter(), status);
                }
            } 
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    void startWithZip(final File tmpFile, final boolean advanced) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                status.stage = 1;
                status.setStatusMessage("Loading ZIP " + tmpFile.getPath());
                try {
                    final Corpus corpus;
                    if (tmpFile.getName().endsWith(".tgz") || tmpFile.getName().endsWith(".tar.gz")) {
                        corpus = CorpusTools.fromTarball(tmpFile);
                    } else {
                        corpus = CorpusTools.fromZIP(tmpFile);
                    }
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        Executor.this.status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig);
                    }
                } catch (Throwable x) {
                    status.failed = true;
                    status.setStatusMessage("Failed: " + x.getMessage());
                    x.printStackTrace();
                }
            }
        }).start();
    }

    void startWithCrawl(final String url, final int maxPages, final boolean domain,
            final boolean advanced) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                status.stage = 1;
                status.setStatusMessage("Crawling " + url);
                try {
                    URL url2 = new URL(url);
                    File f = Files.createTempDir();
                    String crawlStorageFolder = f.getAbsolutePath();
                    Corpus corpus = SaffronCrawler.crawl(crawlStorageFolder, directory,
                            null, maxPages, domain ? "\\w+://\\Q" + url2.getHost() + "\\E.*" : ".*",
                            url, 7);
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        Executor.this.status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig);
                    }
                } catch (Exception x) {
                    status.failed = true;
                    status.setStatusMessage("Failed: " + x.getMessage());
                    x.printStackTrace();
                }
            }
        }).start();
    }

    void startWithJson(final File tmpFile, final boolean advanced) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                status.stage = 1;
                status.setStatusMessage("Reading JSON: " + tmpFile.getPath());
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    IndexedCorpus corpus = mapper.readValue(tmpFile, IndexedCorpus.class);
                    if (advanced) {
                        Executor.this.corpus = corpus;
                        Executor.this.status.advanced = true;
                    } else {
                        execute(corpus, defaultConfig);
                    }
                } catch (Exception x) {
                    status.failed = true;
                    status.setStatusMessage("Failed: " + x.getMessage());
                    x.printStackTrace();
                }
            }
        }).start();
    }

    void execute(Corpus corpus, Configuration config) throws IOException {
        status.advanced = false;
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();

        status.stage++;
        status.setStatusMessage("Indexing Corpus");
        final File indexFile = new File(directory, "index");
        DocumentSearcher searcher = DocumentSearcherFactory.loadSearcher(corpus, indexFile, true);
        status.setStatusMessage("Loading index");
        ArrayList<Document> docs = new ArrayList<>();
        for (Document d : corpus.getDocuments()) {
            docs.add(d);
        }
        IndexedCorpus indexedCorpus = new IndexedCorpus(docs, SaffronPath.fromFile(indexFile));

        status.stage++;
        status.setStatusMessage("Initializing topic extractor");
        //TopicExtraction extractor = new TopicExtraction(config.termExtraction);
        TermExtraction extractor = new TermExtraction(config.termExtraction);

        status.setStatusMessage("Extracting Topics");
        TermExtraction.Result res = extractor.extractTopics(searcher);

        status.setStatusMessage("Writing extracted topics");
        ow.writeValue(new File(directory, "topics-extracted.json"), res.topics);

        status.setStatusMessage("Writing document topic correspondence");
        ow.writeValue(new File(directory, "doc-topics.json"), res.docTopics);
        data.setDocTopics(res.docTopics);

        status.stage++;
        status.setStatusMessage("Extracting authors from corpus");
        Set<Author> authors = Consolidate.extractAuthors(indexedCorpus);

        status.setStatusMessage("Consolidating author names");
        Map<Author, Set<Author>> consolidation = ConsolidateAuthors.consolidate(authors);

        status.setStatusMessage("Applying consoliation to corpus");
        IndexedCorpus corpus2 = applyConsolidation(indexedCorpus, consolidation);

        status.setStatusMessage("Writing consolidated corpus");
        ow.writeValue(new File(directory, "corpus.json"), corpus2);
        data.setCorpus(corpus2);

        status.stage++;
        status.setStatusMessage("Linking to DBpedia");
        // TODO: Even the LinkToDBpedia executable literally does nothing!
        List<Topic> topics = new ArrayList<>(res.topics);
        data.setTopics(res.topics);

        status.setStatusMessage("Saving linked topics");
        ow.writeValue(new File(directory, "topics.json"), topics);

        status.stage++;
        status.setStatusMessage("Connecting authors to topics");
        ConnectAuthorTopic cr = new ConnectAuthorTopic(config.authorTopic);
        Collection<AuthorTopic> authorTopics = cr.connectResearchers(topics, res.docTopics, corpus2.documents);

        status.setStatusMessage("Saving author connections");
        ow.writeValue(new File(directory, "author-topics.json"), authorTopics);
        data.setAuthorTopics(authorTopics);

        status.stage++;
        status.setStatusMessage("Connecting topics");
        TopicSimilarity ts = new TopicSimilarity(config.topicSim);
        final List<TopicTopic> topicSimilarity = ts.topicSimilarity(res.docTopics);

        status.setStatusMessage("Saving topic connections");
        ow.writeValue(new File(directory, "topic-sim.json"), topicSimilarity);
        data.setTopicSim(topicSimilarity);

        status.stage++;
        status.setStatusMessage("Connecting authors to authors");
        AuthorSimilarity as = new AuthorSimilarity(config.authorSim);
        final List<AuthorAuthor> authorSim = as.authorSimilarity(authorTopics);

        status.setStatusMessage("Saving author connections");
        ow.writeValue(new File(directory, "author-sim.json"), authorSim);
        data.setAuthorSim(authorSim);

        status.stage++;
        status.setStatusMessage("Building topic map");
        Map<String, Topic> topicMap = loadMap(topics, mapper);

        //Taxonomy graph = extractTaxonomy(res.docTopics, topicMap);

        status.setStatusMessage("Reading model");
        if (config.taxonomy.modelFile == null) {
            config.taxonomy.modelFile = new SaffronPath("${saffron.home}/models/default.json");
        }
        Model model = mapper.readValue(config.taxonomy.modelFile.toFile(), Model.class);
        
        SupervisedTaxo supTaxo = new SupervisedTaxo(res.docTopics, topicMap, model);
        status.setStatusMessage("Building taxonomy");
        final Taxonomy graph;
        if (topicMap.isEmpty()) {
            graph = new Taxonomy("<EMPTY>", 0, Collections.EMPTY_LIST);
        } else if (config.taxonomy.mode == greedyTrans) {
            TransTaxoExtract taxoExtractor = new TransTaxoExtract(supTaxo, 0.5);
            graph = taxoExtractor.extractTaxonomy(topicMap.keySet());
        } else if (config.taxonomy.mode == greedy) {
            GreedyTaxoExtract taxoExtractor = new GreedyTaxoExtract(supTaxo, config.taxonomy.maxChildren);
            graph = taxoExtractor.extractTaxonomy(res.docTopics, topicMap);
        } else if(config.taxonomy.mode == headAndBag) {
            HeadAndBag taxoExtractor = new HeadAndBag(supTaxo, 0.5);
            graph = taxoExtractor.extractTaxonomy(topicMap.keySet());
        } else {
            MSTTaxoExtract taxoExtractor = new MSTTaxoExtract(supTaxo);
            graph = taxoExtractor.extractTaxonomy(res.docTopics, topicMap);
        }

        status.setStatusMessage("Saving taxonomy");
        ow.writeValue(new File(directory, "taxonomy.json"), graph);
        data.setTaxonomy(graph);

        status.setStatusMessage("Done");
        status.completed = true;
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
