package org.insightcentre.nlp.saffron.domainmodelling;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcenter.nlp.saffron.documentindex.SearchException;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.ExtractionResultsWrapper;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.Document;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KPInfoProcessor;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KPPostRankProcessor;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.Keyphrase;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KeyphraseExtractor;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.TaxonSimilarity;
import org.insightcentre.nlp.saffron.domainmodelling.util.FilterUtils;
import org.insightcentre.nlp.saffron.domainmodelling.util.SaffronMapUtils;

/**
 * The Main executor for Saffron Domain Modeling
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class NewMain {
    private final Configuration config;
    private final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Create a saffron process for the given configuration
     * 
     * @param configuration The configuration
     */
    public NewMain(Configuration configuration) {
        this.config = configuration;
    }
    
    public Map<String, Double> quickRun() throws Exception {
        KeyphraseExtractor ke = new KeyphraseExtractor(config.loadPosExtractor());
        Set<String> stopWords = config.loadStopWords();
        
        DocumentSearcher searcher = config.loadSearcher();

        KPInfoProcessor kpip = new KPInfoProcessor(searcher);
        
        Collection<File> corpus = config.loadCorpus();
        ExtractionResultsWrapper erw = ke.extractPOS(corpus, config.nlp.lengthThreshold);

        
        List<Keyphrase> keyPhrases = ke.extractKeyphrases(searcher, erw.getNounPhraseMap(), 
            config.docsCount(),
            config.nlp.corpusFrequencyThreshold,
            (long)config.nlp.lengthThreshold, stopWords);

        Map<String, Keyphrase> kpMap = mapByKey(keyPhrases);
        
        kpMap = kpip.computeRanks(2, kpMap, config.keyphrase.alpha, config.keyphrase.beta, config.keyphrase.lengthThreshold);

        Map<String, Double> ranks = KPPostRankProcessor.getRankMap(kpMap, stopWords);

        Set<CorpusWord> taxons = new LinkedHashSet<>();

        taxons.addAll(ExtractionResultsWrapper.asCorpusWords(erw.getNounsMap()).getWords());
        taxons.addAll(ExtractionResultsWrapper.asCorpusWords(erw.getVerbsMap()).getWords());
        taxons.addAll(ExtractionResultsWrapper.asCorpusWords(erw.getAdjsMap()).getWords());

        Map<String, Double> domainRanked = new HashMap<>();

        for (CorpusWord taxon : taxons) {
            long occurrences = searcher.numberOfOccurrences(taxon.getString(), searcher.numDocs());
            double domainWordRank = CorpusWord.contextPMIkp(config.docsCount(), config.tokCount(), searcher, new ArrayList<>(ranks.keySet()), taxon.getString(), occurrences);
            domainRanked.put(taxon.getString(), domainWordRank);
        }
        
        return SaffronMapUtils.sortByValuesReverse(domainRanked);
    }
    
    /**
     * Run the configuration
     * @throws Exception 
     */
    public void run() throws Exception {
        KeyphraseExtractor ke = new KeyphraseExtractor(config.loadPosExtractor());
        Set<String> stopWords = config.loadStopWords();
        
        DocumentSearcher searcher = config.loadSearcher();
        
        KPInfoProcessor kpip = new KPInfoProcessor(searcher);
        
        if(config.nlp != null) {
            Map<String, Keyphrase> kpMap = processNLP(ke, searcher, stopWords);
            if (config.keyphrase != null) {
                assignKpDoc(kpMap, kpip, ke, 4, stopWords);

                if (config.kpSim != null) {
                    assignKpSimDoc(kpMap, kpip, searcher, ke, stopWords);
                }

                Map<String,Double> ranks = printKPRanks(kpMap, kpip, 2, stopWords);
                
                rankDomainModel(ranks, searcher);
            }
        }

    }


    /**
     * Apply the NLP processing pipeline
     * @param ke The keyphrase extractor
     * @param stopWords The stop word list
     */
    Map<String, Keyphrase> processNLP(KeyphraseExtractor ke, DocumentSearcher searcher, Set<String> stopWords) throws IOException, SearchException {
        Collection<File> corpus = config.loadCorpus();
        ExtractionResultsWrapper erw = ke.extractPOS(corpus, config.nlp.lengthThreshold);

        
        List<Keyphrase> keyPhrases = ke.extractKeyphrases(searcher, erw.getNounPhraseMap(), 
            config.docsCount(),
            config.nlp.corpusFrequencyThreshold,
            (long)config.nlp.lengthThreshold, stopWords);

        //mapper.writeValue(new File(config.infoFileName), keyPhrases);


        CorpusWordList nouns = ExtractionResultsWrapper.asCorpusWords(erw.getNounsMap());
        nouns.setWords(filter(nouns.getWords(), stopWords));

        mapper.writeValue(new File(config.nlp.nounsFileName), nouns);

        CorpusWordList verbs = ExtractionResultsWrapper.asCorpusWords(erw.getVerbsMap());
        verbs.setWords(filter(verbs.getWords(), stopWords));

        mapper.writeValue(new File(config.nlp.verbsFileName), verbs);

        CorpusWordList adjs = ExtractionResultsWrapper.asCorpusWords(erw.getAdjsMap());
        adjs.setWords(filter(adjs.getWords(), stopWords));

        mapper.writeValue(new File(config.nlp.adjsFileName), adjs);

        return mapByKey(keyPhrases);
    }

    void assignKpDoc(Map<String, Keyphrase> kpMap, KPInfoProcessor kpip, KeyphraseExtractor ke, int rankType, Set<String> stopWords) throws IOException, SearchException {

        //logger.log(Level.INFO, "Assigning keyphrases to documents..");

        double alpha = config.keyphrase.alpha;
        double beta = config.keyphrase.beta;
        int lengthThreshold = config.keyphrase.lengthThreshold;
        kpMap = kpip.computeRanks(rankType, kpMap, alpha, beta, lengthThreshold);


        Map<String, Document> docMap =
                kpip.assignKpToDocsFromFile(true, kpMap, config.keyphrase.rankThreshold, stopWords);

        //logger.log(Level.INFO,
        //        "Output keyphrases for each document with parameter: " + alpha);

        boolean stem = config.keyphrase.stemKeyphrases;

        ke.outputDocKeyphrases(mapper, docMap, new File(config.keyphrase.docsFileName), stem, config.keyphrase.performanceUpto);
    }

    
    void assignKpSimDoc(Map<String, Keyphrase> kpMap, KPInfoProcessor kpip, DocumentSearcher searcher, KeyphraseExtractor ke, Set<String> stopWords) throws IOException, SearchException {

        //logger.log(Level.INFO, "Computing tokens similarity..");
        TaxonSimilarity ts = new TaxonSimilarity();

        //BARRY: So this actually similar to a given set of taxons (e.g. ACM taxonomy)
        //But why??? For evaluation?

        String taxonsFile = config.kpSim.taxonsFileName;

        if (taxonsFile.length() > 0) {
            List<String> taxons = ts.readTaxons(taxonsFile);
            kpMap =
                    kpip.computeTokenSimilarity(searcher, kpMap, taxons, 
                            config.kpSim.taxSimLengthThreshold,
                            config.keyphrase.rankThreshold,
                            config.kpSim.corpusFreqThresholdTaxonSim,
                            config.docsCount(),
                            config.kpSim.containsTaxonBoost,
                            config.kpSim.tokensInCorpus,
                            config.kpSim.taxonSimilaritySlopSize);

        }

        //logger.log(Level.INFO, "Printing termextraction map with tokens similarity..");
         //KPInfoFilesManager.printKeyphraseMaptoFile(kpMap, config.kpSim.tokensSimFileName);
        //mapper.writeValue(new File(config.kpSim.tokensSimFileName), kpMap);


        //logger.log(Level.INFO, "Assigning keyphrases to documents..");

        double alpha = config.keyphrase.alpha;
        double beta = config.keyphrase.beta;
        int lengthThreshold = config.keyphrase.lengthThreshold;

        //Map<String, Keyphrase> kpMap2 = KPInfoFilesManager.readKPMap(new File(config.kpSim.tokensSimFileName), mapper);
        Map<String, Keyphrase> kpMap2 = kpip.computeRanks(1, kpMap, alpha, beta, lengthThreshold);

        Map<String, Document> docMap =
                kpip.assignKpSimToDocsFromFile(true, kpMap2,  config.keyphrase.rankThreshold, stopWords);

        //logger.log(Level.INFO,
        //        "Output keyphrases for each document with parameters: " + alpha + " "
        //                + beta);

        boolean stem = config.keyphrase.stemKeyphrases;

        String outputFile = config.keyphrase.docsFileName;
        ke.outputDocKeyphrases(mapper, docMap, new File(outputFile), stem, config.keyphrase.performanceUpto);
    }

   
    Map<String, Double> printKPRanks(Map<String, Keyphrase> kpMap, KPInfoProcessor kpip, int rankType, Set<String> stopWords) throws IOException {
        double alpha = config.keyphrase.alpha;
        double beta = config.keyphrase.beta;
        int lengthThreshold = config.keyphrase.lengthThreshold;

        kpMap = kpip.computeRanks(rankType, kpMap, alpha, beta, lengthThreshold);

        return KPPostRankProcessor.getRankMap(kpMap, stopWords);

    }

    void rankDomainModel(Map<String, Double> ranks, DocumentSearcher searcher) throws IOException, SearchException, ParseException {

        //BARRY; Testing PMI code
        //LinkedHashMap<String,Double> ranks = SaffronMapUtils.readMapFromFile(new File(config.keyphrase.ranksFileName), 40);

        File nFile = new File(config.nlp.nounsFileName);
        File vFile = new File(config.nlp.verbsFileName);
        File aFile = new File(config.nlp.adjsFileName);

        Set<CorpusWord> taxons = new LinkedHashSet<>();

        taxons.addAll(mapper.readValue(nFile, CorpusWordList.class).getWords());
        taxons.addAll(mapper.readValue(vFile, CorpusWordList.class).getWords());
        taxons.addAll(mapper.readValue(aFile, CorpusWordList.class).getWords());

        Map<String, Double> domainRanked = new HashMap<String, Double>();

        for (CorpusWord taxon : taxons) {
            long occurrences = searcher.numberOfOccurrences(taxon.getString(), searcher.numDocs());
            double domainWordRank = CorpusWord.contextPMIkp(config.docsCount(), config.tokCount(), searcher, new ArrayList<String>(ranks.keySet()), taxon.getString(), occurrences);
            domainRanked.put(taxon.getString(), domainWordRank);
        }
        
        Map<String, Double> topMap = SaffronMapUtils.sortByValuesReverse(domainRanked);
        MapUtils.debugPrint(System.out, "Domain model", topMap);
        System.out.println(StringUtils.join(SaffronMapUtils.sortByValuesReverse(domainRanked).keySet(), "\n"));
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

            NewMain main = new NewMain(config);
            // Run the system
            Map<String, Double> result = main.quickRun();

            main.mapper.writeValue(new File(config.infoFileName), result);
            
            
  
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private Map<String, Keyphrase> mapByKey(List<Keyphrase> keyPhrases) {
        Map<String, Keyphrase> kpMap = new HashMap<>();
        for(Keyphrase kp : keyPhrases) {
            kpMap.put(kp.getString(), kp);
        }
        return kpMap;
    }
}
