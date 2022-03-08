package org.insightcentre.nlp.saffron.authors.connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.config.AuthorTermConfiguration;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.CollectionCorpus;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
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
                accepts("c", "The configuration").withRequiredArg().ofType(File.class);
                accepts("t", "The text corpus").withRequiredArg().ofType(File.class);
                accepts("d", "The document term mapping").withRequiredArg().ofType(File.class);
                accepts("p", "The terms").withRequiredArg().ofType(File.class);
                accepts("o", "The output author-term mapping").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File configurationFile = (File)os.valueOf("c");
            if(configurationFile != null && !configurationFile.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File corpusFile = (File)os.valueOf("t");
            if(corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File docTermFile = (File)os.valueOf("d");
            if(docTermFile == null || !docTermFile.exists()) {
                badOptions(p, "Doc-term do not exist");
            }
            final File termFile = (File)os.valueOf("p");
            if(termFile == null || !termFile.exists()) {
                badOptions(p, "Terms do not exist");
            }

            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();

            AuthorTermConfiguration config          = configurationFile == null ? new AuthorTermConfiguration() : mapper.readValue(configurationFile, Configuration.class).authorTerm;
            Corpus corpus                 = mapper.readValue(corpusFile, CollectionCorpus.class);
            List<DocumentTerm> docTerms = mapper.readValue(docTermFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTerm.class));
            List<Term> terms            = mapper.readValue(termFile, mapper.getTypeFactory().constructCollectionType(List.class, Term.class));

            ConnectAuthorTerm cr = new ConnectAuthorTerm(config);

            Collection<AuthorTerm> authorTerms = cr.connectResearchers(terms, docTerms, corpus.getDocuments());
            
            mapper.writerWithDefaultPrettyPrinter().writeValue(output, authorTerms);
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
