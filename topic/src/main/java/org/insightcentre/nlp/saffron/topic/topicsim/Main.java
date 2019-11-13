package org.insightcentre.nlp.saffron.topic.topicsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.config.TermSimilarityConfiguration;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;

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
                accepts("d", "The document-term alignments").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the output term alignments to").withRequiredArg().ofType(File.class);
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
            final File docTermModelFile = (File)os.valueOf("d");
            if(docTermModelFile == null || !docTermModelFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File outputFile = (File)os.valueOf("o");
            if(outputFile == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();

            TermSimilarityConfiguration config = configuration == null ? new TermSimilarityConfiguration () : mapper.readValue(configuration, TermSimilarityConfiguration.class);
            List<DocumentTerm> docTerms = mapper.readValue(docTermModelFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTerm.class));

            TermSimilarity ts = new TermSimilarity(config);

            ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();
            
            ow.writeValue(outputFile, ts.termSimilarity(docTerms));
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}
