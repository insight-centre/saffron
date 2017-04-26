package org.insightcentre.nlp.saffron.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.DomainModel;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.Topic.MorphologicalVariation;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.topic.gate.TopicExtractorGate;

/**
 * The topic extractor
 * @author John McCrae <john@mccr.ae>
 */
public class Main {
    
    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: "  + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
 
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {{
                accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                accepts("x", "The text corpus to use").withRequiredArg().ofType(File.class);
                accepts("d", "The domain model").withRequiredArg().ofType(File.class);
                accepts("t", "Where to write the topics to").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the document-topic mapping to").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File configuration = (File)os.valueOf("c");
            if(configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File corpusFile = (File)os.valueOf("x");
            if(corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File domainModelFile = (File)os.valueOf("d");
            if(domainModelFile == null || !domainModelFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File outputTopics = (File)os.valueOf("t");
            if(outputTopics == null) {
                badOptions(p, "Output not specified");
            }
            final File outputDocTopics = (File)os.valueOf("o");
            if(outputDocTopics == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            Configuration config = mapper.readValue(configuration, Configuration.class);
            DomainModel   domainModel = mapper.readValue(domainModelFile, DomainModel.class);
            IndexedCorpus        corpus = mapper.readValue(corpusFile, IndexedCorpus.class);
            DocumentSearcher searcher = DocumentSearcherFactory.loadSearcher(corpus);
            

            TopicExtraction extraction = new TopicExtraction(new TopicExtractorGate(config.gateHome), config.maxTopics, config.minTokens, config.maxTokens);

            Map<String, Topic> topics = new HashMap<>();
            List<DocumentTopic> docTopics = new ArrayList<>();
            
            // Load documents through the searcher so the source text is available
            for(Document doc : searcher.allDocuments()) {
                TopicExtraction.Result result = extraction.extractTopics(doc, domainModel.terms, config.getStopWords());
                docTopics.addAll(result.docTopics);
                mergeTopics(topics, result.topics);
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(outputTopics, topics.values());
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputDocTopics, docTopics);
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }

    private static void mergeTopics(Map<String, Topic> topicMap, Set<Topic> topics) {
        for(Topic topic : topics) {
            if(topicMap.containsKey(topic.topicString)) {
                Topic t3 = topicMap.get(topic.topicString);
                List<MorphologicalVariation> mv2 = new ArrayList<>(topic.mvList);
                MV: for(MorphologicalVariation mvx : t3.mvList) {
                    for(MorphologicalVariation mvy : mv2) {
                        if(mvy.equals(mvx))
                            continue MV;
                    }
                    mv2.add(mvx);
                }
                Topic t2 = new Topic(topic.topicString, t3.occurrences + topic.occurrences, t3.matches + topic.matches, topic.score, mv2);
                topicMap.put(t2.topicString, t2);
            } else {
                topicMap.put(topic.topicString, topic);
            }
        }
    }

}
