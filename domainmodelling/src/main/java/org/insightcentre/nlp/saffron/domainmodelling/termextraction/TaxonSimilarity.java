/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;


/**
 * @author Georgeta Bordea
 * 
 */
public class TaxonSimilarity {


  private static final String[] prepositions = { "in", "for", "on", "of" };

  /**
   * Compute the similarity between a taxon and a termextraction in the whole corpus
   * 
   * @param taxon
   *          the taxon (word introducing a termextraction)
   * @param keyphrase
   *          the termextraction candidate
   * @param kpFreq
   *          the frequency of the termextraction candidate in the whole corpus
   * @return a similarity measure for the taxon and termextraction
   */
  public Double computeTaxonSimilarityPrepositions(DocumentSearcher lp, String taxon,
      String keyphrase, Long kpFreq, Integer docsCount) {

    Double pmi = 0.0;

    try {
        String[] queryTaxonPlusPrep = new String[prepositions.length];

        for (int i = 0; i < prepositions.length; i++) {
            queryTaxonPlusPrep[i] = taxon + " " + prepositions[i] + " " + keyphrase;
        }

      Long freqTaxonPlusPrep = new Long(0);
      for (String query : queryTaxonPlusPrep) {
        freqTaxonPlusPrep += lp.numberOfOccurrences(query, docsCount);
      }

      Long freqTaxon = lp.numberOfOccurrences(taxon, docsCount);

      // compute similarity using PMI
      Double pxy = new Double(freqTaxonPlusPrep);
      Double px = new Double(freqTaxon);
      Double py = new Double(kpFreq);

      if ((px != 0) && (py != 0)) {
        pmi = pxy / (px * py);
      }

      return pmi;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0.0;
  }

  public Double computeTaxonSimilarityHead(DocumentSearcher lp, String taxon, String keyphrase,
      Long kpFreq, Integer docsCount) {

    Double pmi = 0.0;



    String queryTaxonHead = keyphrase + " " + taxon;

    try {
      Long freqTaxonHead = lp.numberOfOccurrences(queryTaxonHead, docsCount);

      Long freqTaxon = lp.numberOfOccurrences(taxon, docsCount);

      // compute similarity using PMI
      Double pxy = new Double(freqTaxonHead);
      Double px = new Double(freqTaxon);
      Double py = new Double(kpFreq);

      if ((px != 0) && (py != 0)) {
        pmi = pxy / (px * py);
      }

      return pmi;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0.0;
  }

  /**
   * Use Lucene to compute the occurrence of a termextraction with a taxon in a text
   * window
   * 
   * @param taxon
   * @param keyphrase
   * @param kpFreq
   * @param taxonFreq
   * @return
   */
  private Double computeTaxonSpanSimilarity(DocumentSearcher lp, String taxon, Long taxonFreq,
      String keyphrase, Long kpFreq, Integer docsCount, Long toxensInCorpus,
      Integer spanSlop) {

    Double pmi = 0.0;
    Double fxy = 0.0;
    Double fy = 0.0;


    try {
      Long spanFreq =
          lp.spanOccurrence(taxon, keyphrase, spanSlop, docsCount);

      // compute similarity using PMI
      fxy = new Double(spanFreq);
      Double fx = new Double(taxonFreq);
      fy = new Double(kpFreq);

      if ((fx != 0) && (fy != 0) && (fxy != 0)) {
        pmi = (fxy * toxensInCorpus) / (fx * fy);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // return pmi;
    // return pxy/py;
    // return pxy;

    return Math.log(pmi) / Math.log(2);
  }

  public static Double computeTaxonSimilarity(DocumentSearcher lp, String keyphrase, Long kpFreq,
      List<String> taxons, Integer docsCount, String indexPath) {

    //logger.log(Level.INFO, "Computing taxon similarity for termextraction "
    //    + keyphrase);

    TaxonSimilarity ts = new TaxonSimilarity();
    Double similarity = 0.0;

    // try sum, average
    for (String taxon : taxons) {
      similarity +=
          (ts.computeTaxonSimilarityHead(lp, taxon, keyphrase, kpFreq, docsCount) + 
        	ts.computeTaxonSimilarityPrepositions(lp, taxon, keyphrase, kpFreq, docsCount));
    }

    return similarity;
  }

  public static Double computeTaxonSpanSimilarity(DocumentSearcher lp, String keyphrase,
      Long kpFreq, Map<String, Long> taxonFreqMap, Integer docsCount,
      Integer boost, Long tokensInCorpus, Integer spanSlop) {

    //logger.log(Level.INFO, "Computing taxon similarity for termextraction <"
    //    + keyphrase + ">");

    TaxonSimilarity ts = new TaxonSimilarity();
    Double similarity = 0.0;
    Integer matchedTaxons = 0;
    List<String> taxons = new ArrayList<String>(taxonFreqMap.keySet());

    if (containsTaxon(keyphrase, taxons)) {
      similarity += boost;
    }
    // } else {
    Double taxSim = 0.0;

    // try sum, average
    for (String taxon : taxons) {
      Long taxonFreq = taxonFreqMap.get(taxon);
      taxSim =
          ts.computeTaxonSpanSimilarity(lp, taxon, taxonFreq, keyphrase, kpFreq,
              docsCount, tokensInCorpus, spanSlop);

      if (taxSim > 0) {
        matchedTaxons++;
        similarity += taxSim;
      }
    }
    // }

    //logger.log(Level.INFO, "Matched " + matchedTaxons
    //    + " taxons, similarity = " + similarity);

    Double finalSim = similarity;

    return finalSim;
  }

  public static Double computeNCValue(String keyphrase, Long kpFreq,
      Map<String, Double> taxonWeights, Integer docsCount, Integer spanSlop,
      DocumentSearcher lp) {

    //logger.log(Level.INFO, "Computing taxon similarity for termextraction <"
    //    + keyphrase + ">");

    Double ncValue = 0.0;
    List<String> taxons = new ArrayList<String>(taxonWeights.keySet());

    Double partialNCValue = 0.0;
    for (String taxon : taxons) {
      try {
        partialNCValue =
            lp.spanOccurrence(taxon, keyphrase, spanSlop, docsCount)
                * taxonWeights.get(taxon);
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (partialNCValue > 0) {
        ncValue += partialNCValue;
      }
    }

    return ncValue;
  }

  private static Boolean containsTaxon(String kp, List<String> taxons) {

    Boolean contains = false;
    for (String taxon : taxons) {
      if (kp.contains(taxon)) {
        contains = true;
      }
    }
    return contains;
  }

  public List<String> readTaxons(String fileName) {

    List<String> taxonList = new ArrayList<String>();

    try {
      FileInputStream fstream = new FileInputStream(fileName);
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        taxonList.add(strLine);
      }
      fstream.close();
      in.close();
    } catch (IOException e) {
      //logger.log(Level.ERROR, "IOException caught while reading taxons file "
      //    + fileName + ": " + e.getMessage());
    }

    return taxonList;
  }
}
