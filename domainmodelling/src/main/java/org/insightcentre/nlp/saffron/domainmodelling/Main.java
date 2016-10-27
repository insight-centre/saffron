///**
// *
// */
//package org.insightcentre.nlp.saffron.domainmodelling;
//
//import static org.insightcentre.nlp.saffron.domainmodelling.termextraction.Config.*;
//
//import java.io.File;
//import java.io.IOException;
//import java.text.ParseException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Marshaller;
//import javax.xml.bind.Unmarshaller;
//import org.apache.commons.collections.MapUtils;
//import org.apache.commons.configuration.Configuration;
//import org.apache.commons.configuration.PropertiesConfiguration;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.filefilter.DirectoryFileFilter;
//import org.apache.commons.io.filefilter.TrueFileFilter;
//import org.apache.commons.lang3.StringUtils;
//import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
//import org.insightcenter.nlp.saffron.documentindex.SearchException;
//import org.insightcentre.nlp.saffron.domainmodelling.posextraction.ExtractionResultsWrapper;
//import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSExtractor;
//import org.insightcentre.nlp.saffron.domainmodelling.termextraction.Document;
//import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KPInfoFilesManager;
//import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KPInfoProcessor;
//import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KPPostRankProcessor;
//import org.insightcentre.nlp.saffron.domainmodelling.termextraction.Keyphrase;
//import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KeyphraseExtractor;
//import org.insightcentre.nlp.saffron.domainmodelling.termextraction.TaxonSimilarity;
//import org.insightcentre.nlp.saffron.domainmodelling.util.FilterUtils;
//import org.insightcentre.nlp.saffron.domainmodelling.util.SaffronMapUtils;
//
///**
// * @author Georgeta Bordea
// *
// */
//public class Main {
//
//
//
//    //private static Logger logger = Logger.getLogger(Main.class);
//
//    public static Configuration config = null;
//
//    static KPInfoProcessor kpip;
//
//	private static DocumentSearcher searcher;
//
//    public void run(boolean indexDocs, boolean indexComparativeCorpora,
//                    boolean processNLP, boolean assignKPDocs, boolean assignKpSimDoc,
//                    boolean computeAll, boolean printKPRanks, boolean checkPerf, boolean rankDomainModel,
//                    POSExtractor posExtractor, DocumentSearcher searcher, Set<String> stopWords) 
//                    	throws Exception {
//
//        config = new PropertiesConfiguration(Main.class.getResource("/config.properties"));
//        KeyphraseExtractor ke = new KeyphraseExtractor(posExtractor);
//
////        Directory directory;
////        
////        if (indexDocs || computeAll) {
////        	directory = DocumentIndexFactory.luceneFileDirectory(new File(TaxonUtils.INDEX_PATH), true);
////            indexDocs(directory);
////        } else {
////        	directory = DocumentIndexFactory.luceneFileDirectory(new File(TaxonUtils.INDEX_PATH), false);
////        }
////        
////        searcher = DocumentIndexFactory.luceneSearcher(directory, DocumentIndexFactory.LuceneAnalyzer.LOWERCASE_STEMMER);
//        kpip = new KPInfoProcessor(searcher);
//        
//
//        if (computeAll) {
//            processNLP(ke, stopWords);
//            printKPRanks(2, stopWords);
//            rankDomainModel();
//        }
//
//        if (processNLP) {
//            processNLP(ke, stopWords);
//        }
//
//        if (assignKPDocs) {
//            assignKpDoc(ke, 4, stopWords);
//        }
//
//        if (assignKpSimDoc) {
//            assignKpSimDoc(ke, stopWords);
//        }
//
//        if (printKPRanks) {
//            printKPRanks(2, stopWords);
//        }
//
//        if (rankDomainModel) {
//        	rankDomainModel();
//        }
//        
//    }
//    
//    private static void rankDomainModel() throws IOException, JAXBException, SearchException, ParseException {
//
//        //BARRY; Testing PMI code
//        LinkedHashMap<String,Double> ranks = SaffronMapUtils.readMapFromFile(new File(TaxonUtils.KP_RANKS_FILE_NAME), 40);
//
//        JAXBContext jc = JAXBContext.newInstance(CorpusWordList.class);
//        Unmarshaller unmarshaller = jc.createUnmarshaller();
//
//        File nFile = new File(TaxonUtils.NOUNS_FILE_NAME);
//        File vFile = new File(TaxonUtils.VERBS_FILE_NAME);
//        File aFile = new File(TaxonUtils.ADJS_FILE_NAME);
//
//        Set<CorpusWord> taxons = new LinkedHashSet<>();
//
//        taxons.addAll(((CorpusWordList) unmarshaller.unmarshal(nFile)).getWords());
//        taxons.addAll(((CorpusWordList) unmarshaller.unmarshal(vFile)).getWords());
//        taxons.addAll(((CorpusWordList) unmarshaller.unmarshal(aFile)).getWords());
//
//        Map<String, Double> domainRanked = new HashMap<String, Double>();
//
//        for (CorpusWord taxon : taxons) {
//            Long occurrences = searcher.numberOfOccurrences(taxon.getString(), searcher.numDocs());
//            Double domainWordRank = CorpusWord.contextPMIkp(searcher, new ArrayList<String>(ranks.keySet()), taxon.getString(), occurrences);
//            domainRanked.put(taxon.getString(), domainWordRank);
//        }
//        
//        Map<String, Double> topMap = SaffronMapUtils.sortByValuesReverse(domainRanked);
//        MapUtils.debugPrint(System.out, "Domain model", topMap);
//        System.out.println(StringUtils.join(SaffronMapUtils.sortByValuesReverse(domainRanked).keySet(), "\n"));
//    }
//
//    private static void processNLP(KeyphraseExtractor ke, Set<String> stopWords) throws IOException, JAXBException, SearchException {
//        Collection<File> corpus = allFilesInFolder(config.getString(CORPUS_DIR));
//        ExtractionResultsWrapper erw = ke.extractPOS(corpus, config.getInt(LENGTH_THRESHOLD));
//
//        String keyPhrases = ke.extractKeyphrases(searcher, erw.getNounPhraseMap(), config
//                .getInt(DOCS_COUNT), config
//                .getInt(CORPUS_FREQUENCY_THRESHOLD),
//                config.getLong(LENGTH_THRESHOLD), stopWords);
//
//        FileUtils.writeStringToFile(new File(TaxonUtils.KP_INFO_FILE_NAME), keyPhrases);
//
//        JAXBContext jc = JAXBContext.newInstance(CorpusWordList.class);
//        Marshaller m = jc.createMarshaller();
//        m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
//
//        CorpusWordList nouns = ExtractionResultsWrapper.asCorpusWords(erw.getNounsMap());
//        nouns.setWords(filter(nouns.getWords(), stopWords));
//        File f = new File(TaxonUtils.NOUNS_FILE_NAME);
//        m.marshal(nouns, f);
//
//        CorpusWordList verbs = ExtractionResultsWrapper.asCorpusWords(erw.getVerbsMap());
//        verbs.setWords(filter(verbs.getWords(), stopWords));
//        f = new File(TaxonUtils.VERBS_FILE_NAME);
//        m.marshal(verbs, f);
//
//        CorpusWordList adjs = ExtractionResultsWrapper.asCorpusWords(erw.getAdjsMap());
//        adjs.setWords(filter(adjs.getWords(), stopWords));
//        f = new File(TaxonUtils.ADJS_FILE_NAME);
//        m.marshal(adjs, f);
//    }
//
//    private static List<CorpusWord> filter(List<CorpusWord> words, Set<String> stopWords) {
//        List<CorpusWord> c = new ArrayList<CorpusWord>();
//        for (CorpusWord w : words) {
//            if (FilterUtils.isProperTopic(w.getString(), stopWords) && w.getDocumentFrequency() > 1) {
//                c.add(w);
//            }
//        }
//        return c;
//    }
//    
//    private static Collection<File> allFilesInFolder(String folder) {
//        return FileUtils.listFiles(
//            new File(folder),
//            TrueFileFilter.TRUE,
//            DirectoryFileFilter.DIRECTORY
//        );
//    }
//
////    private static void indexDocs(Directory directory) throws IndexingException, IOException  {
////        Collection<File> corpus = allFilesInFolder(config.getString(CORPUS_DIR));
////        logger.info("Indexing " + corpus.size() + " documents to " + TaxonUtils.INDEX_PATH);
////        DocumentIndexer indexer = DocumentIndexFactory.luceneIndexer(directory, DocumentIndexFactory.LuceneAnalyzer.LOWERCASE_STEMMER);
////        for (File f : corpus) {
////        	indexer.indexDoc(f.getName(), TextExtractor.extractText(f));
////        }
////        indexer.close();
////    }
//
//    private static void assignKpSimDoc(KeyphraseExtractor ke, Set<String> stopWords) throws IOException, SearchException {
//        Map<String, Keyphrase> kpMap =
//                KPInfoFilesManager.readKPMap(new File(TaxonUtils.KP_INFO_FILE_NAME));
//
//        //logger.log(Level.INFO, "Computing tokens similarity..");
//        TaxonSimilarity ts = new TaxonSimilarity();
//
//        //BARRY: So this actually similar to a given set of taxons (e.g. ACM taxonomy)
//        //But why??? For evaluation?
//
//        String taxonsFile = config.getString(TAXONS_FILE_NAME);
//
//        if (taxonsFile.length() > 0) {
//            List<String> taxons = ts.readTaxons(taxonsFile);
//            kpMap =
//                    kpip.computeTokenSimilarity(searcher, kpMap, taxons, config
//                            .getInt(KP_TAX_SIM_LENGTH_THRESHOLD), config
//                            .getDouble(RANK_THRESHOLD), config
//                            .getInt(CORPUS_FREQ_THRESHOLD_TAXON_SIM), config
//                            .getInt(DOCS_COUNT), config
//                            .getInt(KP_SIMILARITY_CONTAINS_TAXON_BOOST), config
//                            .getLong(TOKENS_IN_CORPUS), config
//                            .getInt(KP_TAXON_SIMILARITY_SLOP_SIZE));
//
//        }
//
//        //logger.log(Level.INFO, "Printing termextraction map with tokens similarity..");
//         KPInfoFilesManager.printKeyphraseMaptoFile(kpMap, TaxonUtils.KP_TOKENS_SIM_FILE_NAME);
//
//
//        //logger.log(Level.INFO, "Assigning keyphrases to documents..");
//
//        Double alpha = config.getDouble(ALPHA);
//        Double beta = config.getDouble(BETA);
//        int lengthThreshold = config.getInt(LENGTH_THRESHOLD);
//
//        Map<String, Keyphrase> kpMap2 = KPInfoFilesManager.readKPMap(new File(TaxonUtils.KP_TOKENS_SIM_FILE_NAME));
//        kpMap2 = kpip.computeRanks(1, kpMap2, alpha, beta, lengthThreshold);
//
//        Map<String, Document> docMap =
//                kpip.assignKpSimToDocsFromFile(true, kpMap2, config
//                        .getDouble(RANK_THRESHOLD), stopWords);
//
//        //logger.log(Level.INFO,
//        //        "Output keyphrases for each document with parameters: " + alpha + " "
//        //                + beta);
//
//        Boolean stem = false;
//        if (config.getInt(STEM_KEYPHRASES) == 1) {
//            stem = true;
//        }
//
//        String outputFile = TaxonUtils.KP_DOCS_FILE_NAME;
//        ke.outputDocKeyphrases(docMap, outputFile, true, stem, config.getInt(KP_PERFORMANCE_UPTO));
//    }
//
//    private static void assignKpDoc(KeyphraseExtractor ke, Integer rankType, Set<String> stopWords) throws IOException, SearchException {
//
//        //logger.log(Level.INFO, "Assigning keyphrases to documents..");
//
//        Map<String, Keyphrase> kpMap = KPInfoFilesManager.readKPMap(new File(TaxonUtils.KP_INFO_FILE_NAME));
//        Double alpha = config.getDouble(ALPHA);
//        Double beta = config.getDouble(BETA);
//        int lengthThreshold = config.getInt(LENGTH_THRESHOLD);
//        kpMap = kpip.computeRanks(rankType, kpMap, alpha, beta, lengthThreshold);
//
//
//        Map<String, Document> docMap =
//                kpip.assignKpToDocsFromFile(true, kpMap, config.getDouble(RANK_THRESHOLD), stopWords);
//
//        //logger.log(Level.INFO,
//        //        "Output keyphrases for each document with parameter: " + alpha);
//
//        Boolean stem = false;
//        if (config.getInt(STEM_KEYPHRASES) == 1) {
//            stem = true;
//        }
//
//        String outputFile = TaxonUtils.KP_DOCS_FILE_NAME;
//            ke.outputDocKeyphrases(docMap, outputFile, true, stem, config
//                    .getInt(KP_PERFORMANCE_UPTO));
//    }
//
//
//    public static void printKPRanks(Integer rankType, Set<String> stopWords) throws IOException {
//        Map<String, Keyphrase> kpMap = KPInfoFilesManager.readKPMap(new File(TaxonUtils.KP_INFO_FILE_NAME));
//
//        Double alpha = config.getDouble(ALPHA);
//        Double beta = config.getDouble(BETA);
//        int lengthThreshold = config.getInt(LENGTH_THRESHOLD);
//
//        kpMap = kpip.computeRanks(rankType, kpMap, alpha, beta, lengthThreshold);
//
//
//        File outFile = new File(TaxonUtils.KP_RANKS_FILE_NAME);
//        FileUtils.writeStringToFile(outFile, KPPostRankProcessor.printRanksMap(kpMap, stopWords));
//
//    }
//
//}
