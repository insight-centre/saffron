/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcenter.nlp.saffron.documentindex.SearchException;

import org.insightcentre.nlp.saffron.domainmodelling.TaxonUtils;
import static org.insightcentre.nlp.saffron.domainmodelling.termextraction.Config.ALPHA;
import static org.insightcentre.nlp.saffron.domainmodelling.termextraction.Config.BETA;
import static org.insightcentre.nlp.saffron.domainmodelling.termextraction.Config.LENGTH_THRESHOLD;
import org.insightcentre.nlp.saffron.domainmodelling.util.FilterUtils;
import org.insightcentre.nlp.saffron.domainmodelling.util.SaffronMapUtils;
import org.insightcentre.nlp.saffron.domainmodelling.util.StemUtils;

/**
 * Post-rank processor might as well be a part of the Main class if not for the feckin size of it,
 * so we pass the configuration instance to it rather than passing all the parameters
 * 
 */
public class KPPostRankProcessor {

    private Configuration config;
    private DocumentSearcher lp;

    public KPPostRankProcessor(Configuration config, DocumentSearcher lp) {
        this.config = config;
        this.lp = lp;
    }


  public  void printKpSimRanks(Map<String, Keyphrase> kpMap,
      String outputFile, Set<String> stopWords) throws IOException {

      KPInfoProcessor kpip = new KPInfoProcessor(lp);

      Double alpha = config.getDouble(ALPHA);
      Double beta = config.getDouble(BETA);
      int lengthThreshold = config.getInt(LENGTH_THRESHOLD);

      kpMap = kpip.computeRanks(1, kpMap, alpha, beta, lengthThreshold);

      FileUtils.writeStringToFile(new File(outputFile), printRanksMap(kpMap, stopWords));
  }

  public  void printPostRankSimilarity(DocumentSearcher lp,
      Integer rankType, String kpDocsFile, Map<String, Keyphrase> kpMap, String outputFile) throws IOException, SearchException {

    TaxonSimilarity ts = new TaxonSimilarity();
    List<String> taxons =
        ts.readTaxons(config.getString(Config.TAXONS_FILE_NAME));
    Integer docCount = config.getInt(Config.DOCS_COUNT);

    Map<String, List<String>> docsKp = new HashMap<String, List<String>>();
    try {
      docsKp = readDocKp(kpDocsFile);
    } catch (FileNotFoundException e) {
      //logger.log(Level.WARN, "Could not read the keyphrases for documents");
      e.printStackTrace();
    } catch (IOException e) {
      //logger.log(Level.WARN, "Could not read the keyphrases for documents");
      e.printStackTrace();
    }

    KPInfoProcessor kpip = new KPInfoProcessor(lp);

    List<String> uniqueKP = extractUniqueKP(docsKp);

      Double alpha = config.getDouble(ALPHA);
      Double beta = config.getDouble(BETA);
      int lengthThreshold = config.getInt(LENGTH_THRESHOLD);

    switch (rankType) {
    // Our DSR approach for context words
    case 0:
        kpMap = kpip.computeRanks(0, kpMap, alpha, beta, lengthThreshold);
      docsKp = postRankDSR(lp, kpMap, taxons, docCount, docsKp, uniqueKP);
      break;
    case 1:
      // The NC-value approach for context words
        //BARRY: NC-Value Ranking happens here!!!
        kpMap = kpip.computeRanks(2, kpMap, alpha, beta, lengthThreshold);
      docsKp = postRankNCValue(lp, kpMap, taxons, docCount, docsKp, uniqueKP);
      break;
    default:
      break;
    }

    outputDocs(outputFile, docsKp);
  }

  public  void printPostRankSimilarity(DocumentSearcher lp, 
      Map<String, Keyphrase> kpMap, Map<String, Double> topKPs, String outputFile,
      boolean ncValue) throws IOException, SearchException {

    TaxonSimilarity ts = new TaxonSimilarity();
    List<String> taxons =
        ts.readTaxons(config.getString(Config.TAXONS_FILE_NAME));

    KPInfoProcessor kpip = new KPInfoProcessor(lp);

    Integer docCount = config.getInt(Config.DOCS_COUNT);
    Map<String, Double> resultMap = new HashMap<String, Double>();

    Set<String> topKPSet = topKPs.keySet();
    List<String> topKPList = new ArrayList<String>(topKPSet);

    Map<String, String> stemsMv =
        getMVStringForStems(topKPList, new ArrayList<String>(kpMap.keySet()));

    if (ncValue) {
      Map<String, Double> taxonWeights = loadTaxonWeights(taxons);

      for (String key : topKPList) {
        //logger.log(Level.INFO, "Processing candidate " + key + " ");
        String mv = stemsMv.get(key);
        Double sim =
            TaxonSimilarity.computeNCValue(key, kpMap.get(mv).getFrequency(),
                taxonWeights, docCount, config
                    .getInt(Config.KP_TAXON_SIMILARITY_SLOP_SIZE), lp);

        resultMap.put(key, 0.8 * topKPs.get(key) + 0.2 * sim);
      }
    } else {
      Map<String, Long> taxonsFreqMap = kpip.loadTaxonsFrequencies(taxons);

      for (String key : topKPList) {
        //logger.log(Level.INFO, "Processing candidate " + key + " ");
        String mv = stemsMv.get(key);
        Double sim =
            TaxonSimilarity.computeTaxonSpanSimilarity(lp, mv, kpMap.get(mv)
                .getFrequency(), taxonsFreqMap, docCount, config
                .getInt(Config.KP_SIMILARITY_CONTAINS_TAXON_BOOST), config
                .getLong(Config.TOKENS_IN_CORPUS), config
                .getInt(Config.KP_TAXON_SIMILARITY_SLOP_SIZE));

        // resultMap.put(key, topKPs.get(key) + 3.5 * sim);
        // resultMap.put(key, topKPs.get(key) * (1 + sim));
        resultMap.put(key, sim);
      }
    }

    resultMap = SaffronMapUtils.sortByValues(resultMap);

      FileUtils.writeStringToFile(new File(outputFile), outputKPS(resultMap));
  }

  private  Double computeMax(Map<String, Double> doubleMap) {
    Double max = 0.0;

    Set<String> topKPSet = doubleMap.keySet();
    for (String key : topKPSet) {
      if (doubleMap.get(key) > max) {
        max = doubleMap.get(key);
      }
    }

    return max;
  }

  private  Double computeMin(Map<String, Double> doubleMap) {

    Set<String> topKPSet = doubleMap.keySet();
    List<String> list = new ArrayList<String>(topKPSet);
    Double min = doubleMap.get(list.get(0));

    for (String key : topKPSet) {
      if (doubleMap.get(key) < min) {
        min = doubleMap.get(key);
      }
    }

    return min;
  }

  public  void printCombineWithDSR(DocumentSearcher lp, 
      Map<String, Keyphrase> kpMap, Map<String, Double> topKPs,
      Map<String, Double> dsrScores, String outputFile) throws IOException, SearchException {

    TaxonSimilarity ts = new TaxonSimilarity();
    List<String> taxons =
        ts.readTaxons(config.getString(Config.TAXONS_FILE_NAME));
    Integer docCount = config.getInt(Config.DOCS_COUNT);
    KPInfoProcessor kpip = new KPInfoProcessor(lp);
    Map<String, Long> taxonsFreqMap = kpip.loadTaxonsFrequencies(taxons);

    Map<String, Double> resultMap = new HashMap<String, Double>();

    Set<String> topKPSet = topKPs.keySet();
    List<String> topKPList = new ArrayList<String>(topKPSet);

    Map<String, String> stemsMv =
        getMVStringForStems(topKPList, new ArrayList<String>(kpMap.keySet()));

    Double rank = 0.0;
    Double maxRank = computeMax(topKPs);
    Double maxDSR = computeMax(dsrScores);
    Double minRank = computeMin(topKPs);
    Double minDSR = computeMin(dsrScores);

    for (String key : topKPList) {
      //logger.log(Level.INFO, "Processing candidate " + key + " ");
      rank = topKPs.get(key);
      if (dsrScores.containsKey(key)) {
        // rank = rank + 3.5 * dsrScores.get(key);
        // rank = rank * Math.log(dsrScores.get(key));
        // BEST
        // rank = rank/maxRank + dsrScores.get(key)/maxDSR;
        rank =
            (rank - minRank) / (maxRank - minRank) + (dsrScores.get(key)
                - minDSR) / (maxDSR - minDSR);

      } else {
        Double sim =
            TaxonSimilarity.computeTaxonSpanSimilarity(lp, stemsMv.get(key), kpMap
                .get(stemsMv.get(key)).getFrequency(), taxonsFreqMap, docCount,
                config.getInt(Config.KP_SIMILARITY_CONTAINS_TAXON_BOOST),
                config.getLong(Config.TOKENS_IN_CORPUS), config
                    .getInt(Config.KP_TAXON_SIMILARITY_SLOP_SIZE));

        // rank = rank + 3.5 * sim;
        // rank = rank * Math.log(sim);
        rank =
            (rank - minRank) / (maxRank - minRank) + (sim - minDSR)
                / (maxDSR - minDSR);
      }

      resultMap.put(key, rank);
    }

    resultMap = SaffronMapUtils.sortByValues(resultMap);

      FileUtils.writeStringToFile(new File(outputFile), outputKPS(resultMap));
  }

  public  void printCombineTFIDFWithDSR(DocumentSearcher sd, 
      Map<String, Keyphrase> kpMap, Map<String, Double> topKPs,
      Map<String, Double> dsrScores, String outputFile) throws IOException, SearchException {

    //logger.log(Level.INFO, "Loading frequencies for the domain model..");
    Integer docCount = config.getInt(Config.DOCS_COUNT);

    Map<String, Double> resultMap = new HashMap<String, Double>();

    Set<String> topKPSet = topKPs.keySet();
    List<String> topKPList = new ArrayList<String>(topKPSet);

    //logger.log(Level.INFO, "Reading candidate ranks..");

    //logger.log(Level.INFO, "Collecting topic strings for stems..");
    Map<String, String> stemsMv =
        getMVStringForStems(topKPList, new ArrayList<String>(kpMap.keySet()));

    Map<String, Float> occMap = new HashMap<String, Float>();
    List<String> topicList;

    for (String key : topKPList) {

      //logger.log(Level.INFO, "Processing candidate " + key + " ");
      topicList = new ArrayList<String>();
      topicList.add(stemsMv.get(key));
      try {
        occMap = sd.searchTFIDF(topicList, docCount);
      } catch (Exception e) {
        e.printStackTrace();
      }

      Set<String> keySet = occMap.keySet();

      Float tfidf = new Float(0);
      // Integer docsCount = 0;
      // Double avgTFIDF = 0.0;
      Double sim = 0.0;
      Double rank = 0.0;
      for (String document : keySet) {
        // tfidf += occMap.get(document);
        // docsCount++;

        if (occMap.get(document) > tfidf)
          tfidf = occMap.get(document);
      }
      /*
       * if (docsCount != 0) { avgTFIDF = tfidf / docsCount; }
       * 
       * if (dsrScores.containsKey(key)) { sim = dsrScores.get(key); } else {
       * sim = TaxonSimilarity.computeTaxonSpanSimilarity(stemsMv.get(key),
       * kpMap .get(stemsMv.get(key)).getFrequency(), taxonsFreqMap, docCount,
       * config.getInt(KPUtils.KP_SIMILARITY_CONTAINS_TAXON_BOOST),
       * config.getLong(KPUtils.TOKENS_IN_CORPUS), config
       * .getInt(KPUtils.KP_TAXON_SIMILARITY_SLOP_SIZE), indexPath); }
       */
      // rank = topKPs.get(key) + 3.5 * sim;
      rank = topKPs.get(key) * topKPs.get(key) * tfidf;// avgTFIDF;

      if (sim != 0.0) {
        rank = rank * Math.log(sim);
      }
      resultMap.put(key, rank);
    }

    resultMap = SaffronMapUtils.sortByValues(resultMap);

      FileUtils.writeStringToFile(new File(outputFile), outputKPS(resultMap));
  }

  public  Map<String, Double> readKPRanksMap(String kpFileName) {

    Map<String, Double> kpMap = new HashMap<String, Double>();
    try {
      FileInputStream fstream = new FileInputStream(kpFileName);
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      // Read File Line By Line, do not process first line
      strLine = br.readLine();
      while ((strLine = br.readLine()) != null) {
        if (strLine.contains(",")) {
          String kp = strLine.substring(0, strLine.indexOf(","));
          String value = strLine.substring(strLine.indexOf(",") + 1);

          kpMap.put(kp, Double.parseDouble(value));
        }
      }
      // Close the input stream
      in.close();
    } catch (IOException e) {
      //logger.log(Level.ERROR, "IOException caught while reading file "
       //   + kpFileName + ": " + e.getMessage());
    }

    return kpMap;
  }

  private  Map<String, List<String>> postRankDSR(DocumentSearcher lp, 
      Map<String, Keyphrase> kpMap, List<String> taxons, Integer docCount,
      Map<String, List<String>> docsKp, List<String> uniqueKP) throws IOException, SearchException {
    Map<String, String> kpStemsMap = StemUtils.stemAll(uniqueKP);
    List<String> uniqueStemsKPList = SaffronMapUtils.keysWithUniqueValues(kpStemsMap);
    Collections.sort(uniqueStemsKPList);

    KPInfoProcessor kpip = new KPInfoProcessor(lp);
    Map<String, Long> taxonsFreqMap = kpip.loadTaxonsFrequencies(taxons);

    Map<String, Double> simMap = new HashMap<String, Double>();
    for (String key : uniqueStemsKPList) {
      Keyphrase kp = kpMap.get(key);

      Double sim =
          TaxonSimilarity.computeTaxonSpanSimilarity(lp, key, kp.getFrequency(),
              taxonsFreqMap, docCount, config
                  .getInt(Config.KP_SIMILARITY_CONTAINS_TAXON_BOOST),
              config.getLong(Config.TOKENS_IN_CORPUS), config
                  .getInt(Config.KP_TAXON_SIMILARITY_SLOP_SIZE));
      simMap.put(key, sim);
    }

    simMap = duplicateValues(kpStemsMap, simMap);

    docsKp =
        postRankSim(0, docsKp, simMap, kpMap,
            config.getDouble(Config.BETA), lp, config
                .getInt(Config.DOCS_COUNT));
    return docsKp;
  }

  private  Map<String, List<String>> postRankNCValue(DocumentSearcher lp, 
      Map<String, Keyphrase> kpMap, List<String> taxons,
      Integer docCount, Map<String, List<String>> docsKp, List<String> uniqueKP) throws IOException {

    Map<String, Double> taxonWeights = loadTaxonWeights(taxons);

    Map<String, Double> ncValuesMap = new HashMap<String, Double>();

    for (String key : uniqueKP) {
      Keyphrase kp = kpMap.get(key);

      Double sim =
          TaxonSimilarity.computeNCValue(key, kp.getFrequency(), taxonWeights,
              docCount,
              config.getInt(Config.KP_TAXON_SIMILARITY_SLOP_SIZE), lp);
      ncValuesMap.put(key, sim);
    }

    docsKp =
        postRankSim(1, docsKp, ncValuesMap, kpMap, config
            .getDouble(Config.BETA), lp, config.getInt(Config.DOCS_COUNT));
    return docsKp;
  }

  private  Map<String, Double> loadTaxonWeights(
      List<String> taxons) {

    Map<String, Double> taxonWeights = new HashMap<String, Double>();
    Map<String, Double> nounTaxons =
        loadTaxonsFromFile(TaxonUtils.NOUNS_OUTPUT_FILE_NAME);
    Map<String, Double> verbsTaxons =
        loadTaxonsFromFile(TaxonUtils.VERBS_OUTPUT_FILE_NAME);
    Map<String, Double> adjsTaxons =
        loadTaxonsFromFile(TaxonUtils.ADJS_OUTPUT_FILE_NAME);

    for (String taxon : taxons) {
      if (nounTaxons.containsKey(taxon)) {
        taxonWeights.put(taxon, nounTaxons.get(taxon));
      } else if (verbsTaxons.containsKey(taxon)) {
        taxonWeights.put(taxon, verbsTaxons.get(taxon));
      } else if (adjsTaxons.containsKey(taxon)) {
        taxonWeights.put(taxon, adjsTaxons.get(taxon));
      }
    }
    return taxonWeights;
  }

  private  Map<String, Double> loadTaxonsFromFile(String fileName) {
    Map<String, Double> taxonWeights = new HashMap<String, Double>();

    try {
      FileInputStream fstream = new FileInputStream(fileName);
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        String[] tw = strLine.split(",");
        taxonWeights.put(tw[0], Double.parseDouble(tw[1]));
      }
      fstream.close();
      in.close();
    } catch (IOException e) {
      //logger.log(Level.ERROR, "IOException caught while reading taxons file "
      //    + fileName + ": " + e.getMessage());
    }

    return taxonWeights;
  }

  public  Map<String, Double> duplicateValues(
      Map<String, String> kpStemsMap, Map<String, Double> simMap) {
    Map<String, Double> resultMap = new HashMap<String, Double>();

    Set<String> keys = simMap.keySet();
    for (String key : keys) {
      List<String> sameValueKpMap = sameValueKeysLucene(kpStemsMap, key);
      for (String k : sameValueKpMap) {
        resultMap.put(k, simMap.get(key));
      }
    }

    return resultMap;
  }

  /**
   * Return all the other keys that have the same value as the given key
   * 
   * @param map
   * @param key
   * @return
   */
  public  List<String> sameValueKeysLucene(Map<String, String> map,
      String key) {
    List<String> sameValueKeys = new ArrayList<String>();

    String value = map.get(key);
    Set<String> keys = map.keySet();
    for (String k : keys) {
      if (value.equals(map.get(k))) {
        sameValueKeys.add(k);
      }
    }
    return sameValueKeys;
  }

  private  void outputDocs(String outputFile,
      Map<String, List<String>> docsKp) {
    // Create output file
    BufferedWriter out;
    File f = new File(outputFile);
    FileWriter fstream;

    Set<String> docs = docsKp.keySet();

    // output the documents sorted for performance computation
    List<String> docsList = new ArrayList<String>(docs);
    Collections.sort(docsList);

    try {
      fstream = new FileWriter(f);
      out = new BufferedWriter(fstream);

      for (String docId : docsList) {
        out.write(docId + ": ");

        List<String> keysList = docsKp.get(docId);

        String keysString = "";
        for (String key : keysList) {
          keysString += key + ",";
        }

        out.write(keysString.substring(0, keysString.length() - 1) + "\n");
      }

      out.close();
    } catch (IOException e) {
      //logger.log(Level.WARN,
      //    "Could not compute the post ranking scores based on "
      //        + "taxons similarity");
      e.printStackTrace();
    }
  }

  public static String outputKPS(Map<String, Double> kpMap) {
    // Create output file
    StringBuilder out = new StringBuilder();

    Set<String> keys = kpMap.keySet();

    Map<String, Double> stemmedKPMap = new HashMap<String, Double>();
    for (String key : keys) {
      String stem = StemUtils.stemPhrase(key);

       //BARRY: WHat if two terms have the same stem? We're only choosing the first ranking????
      if (!stemmedKPMap.containsKey(stem)) {
        stemmedKPMap.put(stem, kpMap.get(key));
      }
    }

    stemmedKPMap = SaffronMapUtils.sortByValues(stemmedKPMap);
    keys = stemmedKPMap.keySet();
    // output the documents sorted for performance computation
    List<String> kpList = new ArrayList<String>(keys);
    Collections.reverse(kpList);

      for (String kp : kpList) {
          Double rank = stemmedKPMap.get(kp);
          out.append(kp + "," + rank + "\n");
      }
      return out.toString();
  }

  private  Map<String, List<String>> readDocKp(String kpDocsFile)
      throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(kpDocsFile));
    String cStrLine;

    Map<String, List<String>> docsKp = new HashMap<String, List<String>>();

    // Read File Line By Line
    while ((cStrLine = in.readLine()) != null) {

      String docName = cStrLine.substring(0, cStrLine.indexOf(':'));
      String[] cKeyphrases =
          cStrLine.substring(cStrLine.indexOf(':') + 2).split(",");

      List<String> keysList = new ArrayList<String>();
      for (String key : cKeyphrases) {
        keysList.add(key);
      }

      docsKp.put(docName, keysList);
    }

    in.close();

    return docsKp;
  }

  public  List<String> extractUniqueKP(Map<String, List<String>> kpDocsMap) {
    List<String> uniquekpList = new ArrayList<String>();

    Set<String> docs = kpDocsMap.keySet();
    for (String docId : docs) {
      List<String> docKPList = kpDocsMap.get(docId);

      for (String kp : docKPList) {
        if (!uniquekpList.contains(kp)) {
          uniquekpList.add(kp);
        }
      }
    }

    return uniquekpList;
  }

  private  Map<String, List<String>> postRankSim(Integer rankType,
      Map<String, List<String>> kpDocsMap, Map<String, Double> contextRankMap,
      Map<String, Keyphrase> kps, Double beta, DocumentSearcher lp,
      Integer docCount) {

    Map<String, List<String>> finalKPDocsMap =
        new HashMap<String, List<String>>();
    Set<String> docs = kpDocsMap.keySet();

    for (String docId : docs) {
      Map<String, Double> docSimMap = new HashMap<String, Double>();
      List<String> kpList = kpDocsMap.get(docId);

      for (String key : kpList) {

        Map<String, Float> tfidfMap;
        try {
          tfidfMap = lp.tfidf(key, docCount);

          Double contextRank = contextRankMap.get(key);
          // Remove the space at the end of the id
          Float tfidf = tfidfMap.get(docId.substring(0, docId.length() - 1));
          Double rank = kps.get(key).getRank();

          Double finalRank = 0.0;

          switch (rankType) {
          case 0:
            // DSR post rank
            // TODO use offset zone
            finalRank = (rank + beta * contextRank) * tfidf;
            break;
          case 1:
            // NC-value post rank
            finalRank = (0.8 * rank + 0.2 * contextRank) * tfidf;
            break;

          default:
            break;
          }

          docSimMap.put(key, finalRank);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      kpList = sortKPDesc(docSimMap);

      finalKPDocsMap.put(docId, kpList);
    }

    return finalKPDocsMap;
  }

  private  List<String> sortKPDesc(Map<String, Double> simMap) {
    // print the re-sorted keyphrases
    simMap = SaffronMapUtils.sortByValues(simMap);
    Set<String> keys = simMap.keySet();

    List<String> keysList = new ArrayList<String>();
    for (String key : keys) {
      keysList.add(key);
    }
    Collections.reverse(keysList);
    return keysList;
  }

  public  void reverseKPDocs(String kpDocsFile, String outputFile) {
    // Create output file
    BufferedWriter out;
    File f = new File(outputFile);
    FileWriter fstream;

    try {
      fstream = new FileWriter(f);

      out = new BufferedWriter(fstream);

      BufferedReader in = new BufferedReader(new FileReader(kpDocsFile));
      String cStrLine;

      // Read File Line By Line
      while ((cStrLine = in.readLine()) != null) {

        String docName = cStrLine.substring(0, cStrLine.indexOf(':'));
        String[] cKeyphrases =
            cStrLine.substring(cStrLine.indexOf(':') + 2).split(",");

        List<String> keysList = new ArrayList<String>();
        for (String key : cKeyphrases) {
          keysList.add(key);
        }

        Collections.reverse(keysList);

        out.write(docName + ": ");

        String keysString = "";
        for (String key : keysList) {
          keysString += key + ",";
        }

        out.write(keysString.substring(0, keysString.length() - 1) + "\n");
      }
      out.close();
      in.close();
    } catch (IOException e) {
      //logger.log(Level.WARN,
      //    "Could not compute the post ranking scores based on "
      //        + "taxons similarity");
      e.printStackTrace();
    }
  }

  private  Map<String, String> getMVStringForStems(List<String> stems,
      List<String> kps) {
    Map<String, String> stemMvMap = new HashMap<String, String>();

    for (String stem : stems) {
      for (String key : kps) {
        if (StemUtils.stemPhrase(key).equals(stem)) {
          if (!stemMvMap.containsKey(stem)) {
            stemMvMap.put(stem, key);
            break;
          }
        }
      }
    }

    return stemMvMap;
  }

    /**
     * Print the keyphrases and ranks
     *
     * @throws IOException
     */
    public static Map<String, Double> getRankMap(Map<String, Keyphrase> kpMap, Set<String> stopWords) {
        TreeSet<String> keySet = new TreeSet<String>(kpMap.keySet());

        Map<String, Double> rankMap = new HashMap<String, Double>();

        for (String key : keySet) {
            Keyphrase keyphraseObj = kpMap.get(key);
            if (FilterUtils.isProperTopic(key, stopWords)) {
                rankMap.put(key, keyphraseObj.getRank());
            }
        }

        return rankMap;
    }
  
    @Deprecated
    public static String printRanksMap(Map<String, Keyphrase> kpMap, Set<String> stopWords) throws IOException {
        TreeSet<String> keySet = new TreeSet<String>(kpMap.keySet());

        Map<String, Double> rankMap = new HashMap<String, Double>();

        for (String key : keySet) {
            Keyphrase keyphraseObj = kpMap.get(key);
            if (FilterUtils.isProperTopic(key, stopWords)) {
                rankMap.put(key, keyphraseObj.getRank());
            }
        }
        MapUtils.debugPrint(System.out, "Rank map", rankMap);
        return outputKPS(rankMap);
    }
}
