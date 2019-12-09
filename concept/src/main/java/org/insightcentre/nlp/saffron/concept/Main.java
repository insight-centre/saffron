package org.insightcentre.nlp.saffron.concept;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.insightcentre.nlp.saffron.concept.consolidation.AlgorithmFactory;
import org.insightcentre.nlp.saffron.concept.consolidation.ConceptConsolidation;
import org.insightcentre.nlp.saffron.config.ConceptConsolidationConfiguration;
import org.insightcentre.nlp.saffron.data.Term;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

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
                accepts("t", "The extracter terms").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the output concepts to").withRequiredArg().ofType(File.class);
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
                badOptions(p, "Configuration file does not exist");
            }
            final File termFile = (File)os.valueOf("t");
            if(termFile == null || !termFile.exists()) {
                badOptions(p, "Term file does not exist");
            }
            final File outputFile = (File)os.valueOf("o");
            if(outputFile == null) {
                badOptions(p, "Output not specified");
            }
            
           ObjectMapper mapper = new ObjectMapper();
           ConceptConsolidationConfiguration config = configuration == null ? new ConceptConsolidationConfiguration() : mapper.readValue(configuration, ConceptConsolidationConfiguration.class);
           List<Term> terms = mapper.readValue(termFile, mapper.getTypeFactory().constructCollectionType(List.class, Term.class));
           
           ConceptConsolidation cc = AlgorithmFactory.create(config);
           
           ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();
           ow.writeValue(outputFile, cc.consolidate(terms));
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}
