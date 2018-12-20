package org.insightcentre.nlp.saffron.documentindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;

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
                return;
            }
            final File indexFile = (File)os.valueOf("i");
            
            final Corpus corpus = CorpusTools.readFile(corpusFile);

            DocumentSearcherFactory.index(corpus, indexFile);
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}
