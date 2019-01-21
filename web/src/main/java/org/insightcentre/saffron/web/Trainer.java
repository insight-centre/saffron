package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Model;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.taxonomy.supervised.Features;
import static org.insightcentre.nlp.saffron.taxonomy.supervised.Main.loadMap;
import org.insightcentre.nlp.saffron.taxonomy.supervised.Train;
import org.insightcentre.nlp.saffron.taxonomy.supervised.Train.StringPair;
import org.insightcentre.nlp.saffron.taxonomy.wordnet.Hypernym;
import org.insightcentre.nlp.saffron.term.enrich.EnrichTopics;

/**
 * The code to train a Saffron Instance
 * 
 * @author John McCrae
 */
public class Trainer extends AbstractHandler {

    private Configuration defaultConfig;
    private final File directory;
    private final Status status;

    public Trainer(File directory) {
        this.directory = directory;
        this.status = new Status();
        try {
            this.defaultConfig = new ObjectMapper().readValue(new SaffronPath("${saffron.home}/models/config.json").toFile(), Configuration.class);
        } catch (IOException x) {
            this.defaultConfig = new Configuration();
            System.err.println("Could not load config.json in models folder... using default configuration (" + x.getMessage() + ")");
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
                if (target == null || "/train".equals(target)) {
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
                } else if ("/train/status".equals(target)) {
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

    void execute(Corpus corpus, Taxonomy taxonomy, Configuration config) throws IOException {
        try {
            Model model = new Model();
            model.features = config.taxonomy.features;
            
            status.advanced = false;
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();

            status.stage++;
            status.setStatusMessage("Analyzing Taxonomy");
            List<StringPair> taxo = analyzeTaxononomy(taxonomy);

            status.stage++;
            status.setStatusMessage("Indexing Corpus");
            final File indexFile = new File(directory, "index");
            DocumentSearcher searcher = DocumentSearcherFactory.index(corpus, indexFile, status);

            status.stage++;
            status.setStatusMessage("Enriching topics from corpus");
            Set<String> topics = new HashSet<>();
            flatten(taxonomy, topics);
            EnrichTopics.Result result = EnrichTopics.enrich(topics, searcher, config.termExtraction);

            status.stage++;
            status.setStatusMessage("Building topic map");
            Map<String, Topic> topicMap = loadMap(result.topics, mapper, status);

            status.stage++;
            final Map<String, double[]> glove;
            if (model.features.gloveFile != null) {
                status.setStatusMessage("Loading GloVe Vectors");
                glove = Train.loadGLoVE(model.features.gloveFile.toFile());
            } else {
                status.setStatusMessage("No GloVe Vectors (skipping)");
                glove = null;
            }

            status.stage++;
            final Set<Hypernym> hypernyms;
            if (model.features.hypernyms != null) {
                status.setStatusMessage("Loading hypernyms");
                hypernyms = Train.loadHypernyms(model.features.hypernyms.toFile());
            } else {
                status.setStatusMessage("No hypernyms (skipping)");
                hypernyms = null;
            }

            status.stage++;
            status.setStatusMessage("Initializing Features");
            Features features = new Features(null, null, Train.indexDocTopics(result.docTopics),
                    glove, topicMap, hypernyms, model.features.featureSelection);

            status.stage++;
            if (glove != null) {
                status.setStatusMessage("Learning Hypernym Relation Matrices");
                features = Train.learnSVD(Collections.singletonList(taxo), features, config.taxonomy, model);
            } else {
                status.setStatusMessage("Skipping Hypernym Relation Matrices");
            }

            status.stage++;
            status.setStatusMessage("Building instances");
            svm_problem prob = Train.loadInstances(Collections.singletonList(taxo), features, config.taxonomy.negSampling);
            svm_parameter param = Train.makeParameters();

            libsvm.svm_model svmModel;
            svmModel = libsvm.svm.svm_train(prob, param);
            
            Train.writeClassifier(model, svmModel);
            
            status.stage++;
            status.setStatusMessage("Saving model");
            mapper.writerWithDefaultPrettyPrinter().writeValue(config.taxonomy.modelFile.toFile(), model);
        } catch (Exception x) {
            status.fail(x.getMessage(), x);
        }

    }

    private void flatten(Taxonomy taxonomy, Set<String> topics) {
        topics.add(taxonomy.root);
        for (Taxonomy child : taxonomy.children) {
            flatten(child, topics);
        }
    }

    private List<StringPair> analyzeTaxononomy(Taxonomy taxonomy) {
        List<StringPair> taxos = new ArrayList<>();
        for(Taxonomy child : taxonomy.children) {
            _analyzeTaxonomy(child, taxonomy.root, taxos);
        }
        return taxos;
    }

    private void _analyzeTaxonomy(Taxonomy child, String root, List<StringPair> taxos) {
        taxos.add(new StringPair(child.root, root));
        for(Taxonomy c : child.children) {
            _analyzeTaxonomy(c, child.root, taxos);
        }
    }
    
    public class Status implements SaffronListener {

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
        
        @Override
        public void fail(String message, Throwable cause) {
            this.failed = true;
            setStatusMessage("Failed: " + message);
            cause.printStackTrace();
        }

        @Override
        public void log(String message) {
            System.err.println(message);
        }

        @Override
        public void tick() {
            System.err.print(".");
        }

        @Override
        public void endTick() {
            System.err.println();
        }

    }
}
