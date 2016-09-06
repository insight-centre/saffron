package org.insightcentre.nlp.saffron.domainmodelling;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcenter.nlp.saffron.documentindex.SearchException;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.ExtractionResultsWrapper;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KPInfoProcessor;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KeyphraseExtractor;
import org.insightcentre.nlp.saffron.domainmodelling.util.FilterUtils;

/**
 * The Main executor for Saffron Domain Modeling
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class NewMain {
    private final Configuration config;
    
    /**
     * Create a saffron process for the given configuration
     * 
     * @param configuration The configuration
     */
    public NewMain(Configuration configuration) {
        this.config = configuration;
    }
    
    /**
     * Run the configuration
     * @throws Exception 
     */
    public void run() throws Exception {
        KeyphraseExtractor ke = new KeyphraseExtractor(config.loadPosExtractor());
        Set<String> stopWords = config.loadStopWords();
        
//        Directory directory;
//        
//        if (config.isIndexDocs()) {
//        	directory = DocumentIndexFactory.luceneFileDirectory(new File(config.getIndexPath()), true);
//            indexDocs(directory);
//        } else {
//        	directory = DocumentIndexFactory.luceneFileDirectory(new File(config.getIndexPath()), false);
//        }
//        
//        DocumentSearcher searcher = DocumentIndexFactory.luceneSearcher(directory, DocumentIndexFactory.LuceneAnalyzer.LOWERCASE_STEMMER);
        DocumentSearcher searcher = null;
        
        KPInfoProcessor kpip = new KPInfoProcessor(searcher);
        
        if(config.nlp != null) {
            processNLP(ke, searcher, stopWords);
        }

    }


    /**
     * Apply the NLP processing pipeline
     * @param ke The keyphrase extractor
     * @param stopWords The stop word list
     */
    private void processNLP(KeyphraseExtractor ke, DocumentSearcher searcher, Set<String> stopWords) throws IOException, SearchException {
        Collection<File> corpus = config.loadCorpus();
        ExtractionResultsWrapper erw = ke.extractPOS(corpus, config.nlp.getLengthThreshold());

        String keyPhrases; 
        keyPhrases = ke.extractKeyphrases(searcher, erw.getNounPhraseMap(), 
            config.nlp.getDocsCount(),
            config.nlp.getCorpusFrequencyThreshold(),
            (long)config.nlp.getLengthThreshold(), stopWords);

        ObjectMapper mapper = new ObjectMapper();

        mapper.writeValue(new File(config.nlp.getKpInfoFileName()), keyPhrases);


        CorpusWordList nouns = ExtractionResultsWrapper.asCorpusWords(erw.getNounsMap());
        nouns.setWords(filter(nouns.getWords(), stopWords));

        mapper.writeValue(new File(config.nlp.getNounsFileName()), nouns);

        CorpusWordList verbs = ExtractionResultsWrapper.asCorpusWords(erw.getVerbsMap());
        verbs.setWords(filter(verbs.getWords(), stopWords));

        mapper.writeValue(new File(config.nlp.getVerbsFileName()), verbs);

        CorpusWordList adjs = ExtractionResultsWrapper.asCorpusWords(erw.getAdjsMap());
        adjs.setWords(filter(adjs.getWords(), stopWords));

        mapper.writeValue(new File(config.nlp.getAdjsFileName()), adjs);
    }

    private static List<CorpusWord> filter(List<CorpusWord> words, Set<String> stopWords) {
        List<CorpusWord> c = new ArrayList<>();
        for (CorpusWord w : words) {
            if (FilterUtils.isProperTopic(w.getString(), stopWords) && w.getDocumentFrequency() > 1) {
                c.add(w);
            }
        }
        return c;
    }


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

            // Read configuration
            Configuration config = new ObjectMapper().readValue(configuration, Configuration.class);

            // Run the system
            new NewMain(config).run();
            
  
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
