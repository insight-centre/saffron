package org.insightcentre.nlp.saffron.taxonomy.supervised;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.Model;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxonomySearch;

/**
 * Create a taxonomy based on a supervised model
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Main {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("d", "The document term alignment").withRequiredArg().ofType(File.class);
                    accepts("t", "The terms to load").withRequiredArg().ofType(File.class);
                    accepts("o", "Where to write the output taxonomy").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            final File configuration = (File) os.valueOf("c");
            if (configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File docTermFile = (File) os.valueOf("d");
            if (docTermFile != null && !docTermFile.exists()) {
                badOptions(p, "Doc-term file does not exist");
            }
            final File termFile = (File) os.valueOf("t");
            if (termFile == null || !termFile.exists()) {
                badOptions(p, "Term file does not exist");
            }
            final File output = (File) os.valueOf("o");
            if (output == null) {
                badOptions(p, "Output not specified");
            }

            ObjectMapper mapper = new ObjectMapper();

            // Read configuration
            Configuration config = mapper.readValue(configuration, Configuration.class);
            if (config.taxonomy == null || config.taxonomy.modelFile == null
                    || config.taxonomy.search == null) {
                badOptions(p, "Configuration does not have a model file");
            }
            List<DocumentTerm> docTerms = mapper.readValue(docTermFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTerm.class));
            List<Term> terms = mapper.readValue(termFile, mapper.getTypeFactory().constructCollectionType(List.class, Term.class));

            Map<String, Term> termMap = loadMap(terms, mapper, new DefaultSaffronListener());
            
            Model model = mapper.readValue(config.taxonomy.modelFile.toFile(), Model.class);

            SupervisedTaxo supTaxo = new SupervisedTaxo(docTerms, termMap, model);
            TaxonomySearch search = TaxonomySearch.create(config.taxonomy.search, supTaxo, termMap.keySet());
            final Taxonomy graph = search.extractTaxonomy(termMap);

            mapper.writerWithDefaultPrettyPrinter().writeValue(output, graph);

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    public static Map<String, Term> loadMap(List<Term> terms, ObjectMapper mapper, SaffronListener log) throws IOException {
        Map<String, Term> tMap = new HashMap<>();
        for (Term term : terms) {
            tMap.put(term.getString(), term);
        }
        return tMap;
    }

}
