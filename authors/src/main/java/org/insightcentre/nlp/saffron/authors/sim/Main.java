package org.insightcentre.nlp.saffron.authors.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.config.AuthorSimilarityConfiguration;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
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
                accepts("c", "The configuration to use (optional)").withRequiredArg().ofType(File.class);
                accepts("d", "The author-topic alignments").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the output author alignments to").withRequiredArg().ofType(File.class);
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
            final File docTopicModelFile = (File)os.valueOf("d");
            if(docTopicModelFile == null || !docTopicModelFile.exists()) {
                badOptions(p, "Alignment does not exist");
            }
            final File outputFile = (File)os.valueOf("o");
            if(outputFile == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();

            AuthorSimilarityConfiguration config = configuration == null ? new AuthorSimilarityConfiguration () : mapper.readValue(configuration, AuthorSimilarityConfiguration.class);
            List<AuthorTerm> docTopics = mapper.readValue(docTopicModelFile, mapper.getTypeFactory().constructCollectionType(List.class, AuthorTerm.class));

            AuthorSimilarity ts = new AuthorSimilarity(config);

            ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();
            
            ow.writeValue(outputFile, ts.authorSimilarity(docTopics));
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }

    public static class Configuration {
        public int top_n = 50;
        public double threshold = 0.1;
    }

}
