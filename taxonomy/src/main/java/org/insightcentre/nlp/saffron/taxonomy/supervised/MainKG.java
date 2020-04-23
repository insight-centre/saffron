package org.insightcentre.nlp.saffron.taxonomy.supervised;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.taxonomy.classifiers.BERTBasedRelationClassifier;
import org.insightcentre.nlp.saffron.taxonomy.search.KGSearch;

import com.fasterxml.jackson.databind.ObjectMapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class MainKG {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
    
	public static void main(String[] args) throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("d", "The document term alignment").withRequiredArg().ofType(File.class);
                    accepts("t", "The terms to load").withRequiredArg().ofType(File.class);
                    accepts("o", "Where to write the output knowledge graph").withRequiredArg().ofType(File.class);
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
            
            BERTBasedRelationClassifier relationClassifier = new BERTBasedRelationClassifier(config.kg.kerasModelFile.getResolvedPath(), config.kg.bertModelFile.getResolvedPath());

            KGSearch search = KGSearch.create(config.taxonomy.search, config.kg, relationClassifier, termMap.keySet());
            final KnowledgeGraph graph = search.extractKnowledgeGraph(termMap);

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

