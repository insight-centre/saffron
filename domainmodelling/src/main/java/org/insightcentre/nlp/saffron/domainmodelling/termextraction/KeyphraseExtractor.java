/**
 *
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;

import ie.deri.unlp.javaservices.documentindex.DocumentSearcher;
import ie.deri.unlp.javaservices.documentindex.SearchException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.DocUtils;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.DocumentProcessor;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.ExtractionResultsWrapper;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSBearer;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSExtractor;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSExtractorDocumentProcessor;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.StatProcessor;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.UnsupportedFileTypeException;
import org.insightcentre.nlp.saffron.domainmodelling.util.FilterUtils;
import org.insightcentre.nlp.saffron.domainmodelling.util.StemUtils;

/**
 * @author Georgeta Bordea
 *
 *         Class used to process a corpus and extract and rank keyphrases for
 *         each document
 */
public class KeyphraseExtractor implements StatProcessor {

    private final POSExtractor posExtractor;

    public KeyphraseExtractor(POSExtractor posExtractor) {
        this.posExtractor = posExtractor;
    }


    /**
     * @param docPath
     *
     *          0: nouns 1: verbs 2: adjectives
     *
     * @return
     */
    public ExtractionResultsWrapper extractNLPInfo(String docPath,
                                                   Integer lengthThresh) {

        ExtractionResultsWrapper erw =
                new ExtractionResultsWrapper(new HashMap<String, Keyphrase>(),
                        new HashMap<String, Long>(), new HashMap<String, Long>(),
                        new HashMap<String, Long>(), 0, 0);

        final File f = new File(docPath);
        final long fileLength = f.length();

        if (!docPath.endsWith(".pdf") && !docPath.endsWith(".txt")) {
            //logger.log(Level.INFO,
            //        "Only pdf and txt files are processed, ignoring file " + docPath
            //                + " .. ");
        } else if (fileLength > 100) {
            POSBearer posBearer = null;
            try {
                //String offer = docProvider.offer(docPath);
                URL offer = new File(docPath).toURI().toURL();
                posBearer = posExtractor.getPOSFromUrl(offer.toString());
            } catch (UnsupportedFileTypeException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (MalformedURLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                throw new RuntimeException(e);
            }
            

            Map<String, Long> stringMap = new HashMap<String, Long>();

            if (posBearer != null) {

                Map<String, Integer> nounPhraseMap = posBearer.getNounphraseMap();
                Map<String, Keyphrase> npMap = erw.getNounPhraseMap();

                Set<String> keySet = nounPhraseMap.keySet();
                for (String nounPhrase : keySet) {
                    String np = nounPhrase.toLowerCase();
                    if (!npMap.containsKey(np)) {
                        npMap.put(np, new Keyphrase(nounPhrase, nounPhraseMap
                                .get(nounPhrase), null, null, null, null));
                    }
                }
                erw.setNounPhraseMap(npMap);

                List<String> nounList = posBearer.getNounList();
                stringMap = erw.getNounsMap();
                for (String noun : nounList) {
                    if (stringMap.containsKey(noun)) {
                        stringMap.put(noun, stringMap.get(noun) + 1);
                    } else {
                        stringMap.put(noun, new Long(1));
                    }
                }
                erw.setNounsMap(stringMap);

                List<String> verbList = posBearer.getVerbList();
                stringMap = erw.getVerbsMap();
                for (String verb : verbList) {
                    if (stringMap.containsKey(verb)) {
                        stringMap.put(verb, stringMap.get(verb) + 1);
                    } else {
                        stringMap.put(verb, new Long(1));
                    }
                }
                erw.setVerbsMap(stringMap);

                List<String> adjList = posBearer.getAdjectiveList();
                stringMap = erw.getAdjsMap();
                for (String adj : adjList) {
                    if (stringMap.containsKey(adj)) {
                        stringMap.put(adj, stringMap.get(adj) + 1);
                    } else {
                        stringMap.put(adj, new Long(1));
                    }
                }
                erw.setAdjsMap(stringMap);
                erw.setTokensNo(posBearer.getTokensNo());

                return erw;
            }
        } else {
        	//logger.warn("Skipping file " + docPath + ", less than 100 bytes");
        }
        return null;
    }

    /**
     * Initialise gate and process all documents from the corpus
     *
     * @param ke
     * @return
     */
    public ExtractionResultsWrapper extractPOS(Collection<File> files, int lengthThreshold) {
        DocUtils du = new DocUtils();
        DocumentProcessor dp = new POSExtractorDocumentProcessor();
        du.processDocs(files,
                dp, lengthThreshold, this);

        return du.getErw();
    }



    /**
     * Build a map with termextraction objects for all the strings extracted with Gate.
     * Compute embeddedness and overall frequency.
     *
     * @param keyphraseStringMap
     * @param countDocs
     * @return
     * @throws IOException
     * @throws SearchException 
     */
    public String extractKeyphrases(DocumentSearcher lp, Map<String, Keyphrase> keyphraseStringMap,
                                    Integer countDocs, Integer corpusFreqThreshold,
                                    Long lengthMax, Set<String> stopWords) throws IOException, SearchException {

        StringBuilder out = new StringBuilder();

        //logger.log(Level.INFO, "Extracted " + keyphraseStringMap.size()
        //        + " termextraction candidates");

        List<String> keyList = new ArrayList<String>(keyphraseStringMap.keySet());
        Collections.sort(keyList);

        Set<String> embAllKeyphrasesSet =
                KPInfoProcessor.extractParsedEmbKP(lp, keyphraseStringMap, 3);

        for (String key : keyList) {
            Integer length = keyphraseStringMap.get(key).getLength();
            if (FilterUtils.isProperTopic(key, stopWords) && length <= lengthMax) {

                Long frequency = new Long(0);
                frequency = lp.numberOfOccurrences(key, countDocs);

                if (frequency > corpusFreqThreshold) {
                    out.append(key + ",");
                    out.append(length + ",");
                    out.append(frequency + ",");
                    out.append("0,");
                    //logger.log(Level.INFO, "Computing embeddeness for: " + key);

                    Integer emb = 0;
                    // compute embeddedness only for multi-word keyphrases,
                    // don't check for maximum length topics
                    if ((length > 1) && (length < lengthMax)) {
                        emb = KPInfoProcessor.computeEmbeddedness(lp, key, embAllKeyphrasesSet);
                    }
                    out.append(emb + "\n");
                    //logger.log(Level.INFO, "Done..");
                }
            }
        }
        return out.toString();
    }

    /**
     * Output the extracted keyphrases
     *
     * @param docMap
     * @throws IOException
     */
    public void outputDocKeyphrases(Map<String, Document> docMap,
                                    String outputFile, Boolean cutTopKP, Boolean stem, Integer upto)
            throws IOException, SearchException {

        BufferedWriter out = null;

        // Create file
        File f = new File(outputFile);
        FileWriter fstream;
        try {
            fstream = new FileWriter(f);

            out = new BufferedWriter(fstream);

            // List<String> docList =
            // PerformanceEvaluator.loadDocIdsFromFile(KPUtils.TEST_RESULTS_FILE);

            Set<String> docList = docMap.keySet();
            List<String> docs = new ArrayList<String>(docList);
            Collections.sort(docs);

            for (String docId : docs) {
                if (!docId.endsWith(".txt")) {
                    docId += ".txt";
                }
                out.write(docId + " : ");// .substring(0, docId.length() - 4) + " : ");

                Document doc = docMap.get(docId);
                if (doc != null) {

                    Map<String, Double> keyphraseMap = doc.getKeyphraseRankMap();

                    // System.out.println(docId + "\t" + keyphraseMap.size());

                    // List<String> kpList = KPInfoProcessor.cutTopKp(keyphraseMap, upto);
                    List<String> kpList =
                            KPInfoProcessor.cutTopKpUniqueRoot(keyphraseMap, upto);

                    if (stem) {
                        kpList = stemList(kpList);
                    }

                    out.write(StringUtils.join(kpList, ',') + "\n");
                }
            }
        } finally {
            out.close();
        }
    }

    /**
     * Stem keyphrases in the list, removing duplicates
     *
     * @param list
     * @return
     */
    private List<String> stemList(List<String> list) {

        List<String> tmpList = new ArrayList<String>();
        for (String kp : list) {
            String tmpKp = StemUtils.stemPhrase(kp);

            if (!tmpList.contains(tmpKp)) {
                tmpList.add(tmpKp);
            }
        }
        return tmpList;
    }

}
