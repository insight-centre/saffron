package org.insightcentre.nlp.saffron.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.taxonomy.graph.DirectedGraph;

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
                accepts("t", "The text corpus to load").withRequiredArg().ofType(File.class);
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
            if(configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File corpusFile  = (File)os.valueOf("t");
            if(corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            Configuration config = mapper.readValue(configuration, Configuration.class);
            Corpus corpus        = mapper.readValue(corpusFile, Corpus.class);

            DocumentSearcher searcher = DocumentSearcherFactory.loadSearcher(corpus);

            Map<String, Topic> topicMap = loadMap(config.topics, mapper);

            DirectedGraph<Topic> graph = TaxonomyConstructor.optimisedSimilarityGraph(searcher, config.simThreshold, config.spanSize, topicMap, config.minCommonDocs);

            mapper.writerWithDefaultPrettyPrinter().writeValue(output, graph);
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private static Map<String, Topic> loadMap(File topics, ObjectMapper mapper) throws IOException {
        Map<String, Object> map1 = mapper.readValue(topics, Map.class);
        Map<String, Topic> map2 = new HashMap<>();
        for(Map.Entry<String, Object> e : map1.entrySet()) {
            map2.put(e.getKey(), mapper.convertValue(e.getValue(), Topic.class));
        }
        return map2;
    }

    private static String loadText(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while((line = br.readLine()) != null) {
            sb.append(line).append(" ");
        }
        return sb.toString();
    }


}
