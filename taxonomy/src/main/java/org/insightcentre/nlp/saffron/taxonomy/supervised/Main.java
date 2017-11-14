package org.insightcentre.nlp.saffron.taxonomy.supervised;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration.Mode;

/**
 * Create a taxonomy based on a supervised model
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
                accepts("d", "The document topic alignment").withRequiredArg().ofType(File.class);
                accepts("t", "The topics to load").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the output taxonomy").withRequiredArg().ofType(File.class);
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
            Configuration config = mapper.readValue(configuration, Configuration.class);
            if(config.taxonomy == null || config.taxonomy.modelFile == null ||
                    config.taxonomy.mode == null) {
                badOptions(p, "Configuration does not have a model file");
            }
            List<DocumentTopic> docTopics = mapper.readValue(docTopicFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTopic.class));
            List<Topic> topics   = mapper.readValue(topicFile, mapper.getTypeFactory().constructCollectionType(List.class, Topic.class));

            Map<String, Topic> topicMap = loadMap(topics, mapper);
            
            SupervisedTaxo supTaxo = new SupervisedTaxo(config.taxonomy, docTopics, topicMap);
            final Taxonomy graph;
            if(config.taxonomy.mode == Mode.greedy) {
                GreedyTaxoExtract extractor = new GreedyTaxoExtract(supTaxo, config.taxonomy.maxChildren);
                graph = extractor.extractTaxonomy(docTopics, topicMap);
            } else {
                MSTTaxoExtract extractor = new MSTTaxoExtract(supTaxo);
                graph = extractor.extractTaxonomy(docTopics, topicMap);
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(output, graph);
            
        } catch(Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
            
     public static Map<String, Topic> loadMap(List<Topic> topics, ObjectMapper mapper) throws IOException {
        Map<String, Topic> tMap = new HashMap<>();
        //System.err.printf("%d topics\n", topics.size());
        for(Topic topic : topics) 
            tMap.put(topic.topicString, topic);
        return tMap;
    }
     
}
