package org.insightcentre.nlp.saffron.topic.atr4s;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcherFactory;

/**
 * Main point for the ATR4S-based extract-topics command
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Main {

    public static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            OptionParser p = new OptionParser() {
                {
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("x", "The text corpus to use").withRequiredArg().ofType(File.class);
                    accepts("t", "Where to write the topics to").withRequiredArg().ofType(File.class);
                    accepts("o", "Where to write the document-topic mapping to").withRequiredArg().ofType(File.class);
                }
            };

            final OptionSet os;
            try {
                os = p.parse(args);
            } catch (Throwable x) {
                badOptions(p, x.getMessage());
                return;
            }
            File configuration = (File) os.valueOf("c");
            if (configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            File corpusFile = (File) os.valueOf("x");
            if (corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            File outputTopics = (File) os.valueOf("t");
            if (outputTopics == null) {
                badOptions(p, "Output not specified");
            }
            File outputDocTopics = (File) os.valueOf("o");
            if (outputDocTopics == null) {
                badOptions(p, "Output not specified");
            }
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            Configuration config = mapper.readValue(configuration, Configuration.class);
            IndexedCorpus corpus = mapper.readValue(corpusFile, IndexedCorpus.class);
            DocumentSearcher searcher = DocumentSearcherFactory.loadSearcher(corpus);

            TopicExtraction extractor = new TopicExtraction(config);

            TopicExtraction.Result res = extractor.extractTopics(searcher);

            mapper.writerWithDefaultPrettyPrinter().writeValue(outputTopics, res.topics);
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputDocTopics, res.docTopics);

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
    
    public enum WeightingMethod {
        one, voting, puatr
    };
    
    public enum Feature {
        weirdness, avgTermFreq, residualIdf, totalTfIdf, cValue, basic, comboBasic, postRankDC, relevance, domainCoherence, domainPertinence, novelTopicModel, linkProbability, keyConceptRelatedness
    };

    public static class Configuration {

        public double threshold = 0.0;
        public int maxTopics = 1000;
        public int ngramMin = 1;
        public int ngramMax = 4;
        public int minTermFreq = 2;
        public WeightingMethod method = WeightingMethod.one;
        public List<Feature> features = java.util.Arrays.asList(Feature.novelTopicModel,
                Feature.cValue, Feature.relevance, Feature.linkProbability,
                Feature.domainCoherence, Feature.keyConceptRelatedness);
        public String corpus;
        public String infoMeasure;
        public String w2vmodelPath;
        public Feature baseFeature = Feature.comboBasic;

    }
}
