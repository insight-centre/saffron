package org.insightcentre.nlp.saffron.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 *
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
                //accepts("x", "The text corpus to load").withRequiredArg().ofType(File.class);
                accepts("d", "The document topic alignment").withRequiredArg().ofType(File.class);
                accepts("t", "The topics to load").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the domain model").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File configuration = (File)os.valueOf("c");
            if(configuration != null && !configuration.exists()) {
                badOptions(p, "Configuration does not exist");
            }
//            final File corpusFile  = (File)os.valueOf("x");
//            if(corpusFile == null || !corpusFile.exists()) {
//                badOptions(p, "Corpus does not exist");
//            }
            final File docTopicFile = (File)os.valueOf("d");
            if(docTopicFile != null && !docTopicFile.exists()) {
                badOptions(p, "Doc-topic file does not exist");
            }
            final File topicFile  = (File)os.valueOf("t");
            if(topicFile == null || !topicFile.exists()) {
                badOptions(p, "Topic file does not exist");
            }
            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            Configuration config = configuration == null ? new Configuration() : mapper.readValue(configuration, Configuration.class);
            //Corpus corpus        = mapper.readValue(corpusFile, Corpus.class);
            List<DocumentTopic> docTopics = mapper.readValue(docTopicFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTopic.class));
            List<Topic> topics   = mapper.readValue(topicFile, mapper.getTypeFactory().constructCollectionType(List.class, Topic.class));

//            DocumentSearcher searcher = DocumentSearcherFactory.loadSearcher(corpus);

            Map<String, Topic> topicMap = loadMap(topics, mapper);

            //Taxonomy graph = TaxonomyConstructor.optimisedSimilarityGraph(docTopics, config.simThreshold, config.spanSize, topicMap, config.minCommonDocs);
            //Taxonomy graph = SimpleTaxonomy.optimisedSimilarityGraph(docTopics, topicMap);
            Taxonomy graph = new GreedyBinary(1.0, 1.0).optimisedSimilarityGraph(docTopics, topicMap);

            mapper.writerWithDefaultPrettyPrinter().writeValue(output, graph);
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private static Map<String, Topic> loadMap(List<Topic> topics, ObjectMapper mapper) throws IOException {
        Map<String, Topic> tMap = new HashMap<>();
        for(Topic topic : topics) 
            tMap.put(topic.topicString, topic);
        return tMap;
    }
}
