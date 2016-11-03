package org.insightcentre.nlp.saffron.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
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
                accepts("o", "Where to write the topics to").withRequiredArg().ofType(File.class);
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
            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            Configuration config = mapper.readValue(configuration, Configuration.class);

            TopicExtraction extraction = new TopicExtraction(new TopicExtractorGate(config.gateHome), config.maxTopics, config.minTokens, config.maxTokens);

            Map<String, Set<Topic>> result = new HashMap<>();
            
            for(File f : config.loadCorpus()) {
                String docText = loadText(f);

                result.put(f.getName(), extraction.extractTopics(docText, config.getDomainModel(), config.getStopWords()));
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(output, result);
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

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
