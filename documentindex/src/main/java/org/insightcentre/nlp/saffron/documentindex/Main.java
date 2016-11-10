package org.insightcentre.nlp.saffron.documentindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.security.auth.login.Configuration;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Corpus;
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
                accepts("c", "The file containing the corpus metadata OR a folder all files of which will be automatically indexed").withRequiredArg().ofType(File.class);
                accepts("i", "The file to write the index to").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the output corpus metadata file to").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File corpusFile = (File)os.valueOf("c");
            if(corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File indexFile = (File)os.valueOf("i");
            final File outputFile = (File)os.valueOf("o");
            
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            Corpus corpus;
            
            if(corpusFile.isDirectory()) {
                if(indexFile == null) {
                    badOptions(p, "Corpus file is a folder but index file is null");
                }
                if(outputFile == null) {
                    badOptions(p, "Corpus file is a folder but output file not specified");
                }
                corpus = Corpus.fromFolder(corpusFile, indexFile);
                mapper.writeValue(outputFile, corpus);
            } else if(corpusFile.getName().endsWith(".zip")) {
                if(indexFile == null) {
                    badOptions(p, "Corpus file is a ZIP but index file is null");
                }
                if(outputFile == null) {
                    badOptions(p, "Corpus file is a ZIP but output file not specified");
                }
                corpus = Corpus.fromZIP(corpusFile, indexFile);
                mapper.writeValue(outputFile, corpus);
             } else {
                corpus = mapper.readValue(corpusFile, Corpus.class);
                if(indexFile != null && !indexFile.equals(corpus.index)) {
                    System.err.println("Using " + indexFile + " as index not " + corpus.index + " as in metadata");
                    corpus = new Corpus(corpus.documents, indexFile);
                }
            }

            DocumentSearcherFactory.loadSearcher(corpus, true);
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}
