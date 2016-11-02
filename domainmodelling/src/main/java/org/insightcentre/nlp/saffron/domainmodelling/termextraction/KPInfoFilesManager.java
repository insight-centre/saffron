/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.domainmodelling.util.SaffronMapUtils;


/**
 * @author Georgeta Bordea
 * 
 */
public class KPInfoFilesManager {

  protected static Map<String, List<String>> readDocMap(String file) {
    Map<String, List<String>> kpMap = new HashMap<String, List<String>>();
    FileInputStream fstream;
    try {
      fstream = new FileInputStream(file);

      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;

      while ((strLine = br.readLine()) != null) {

        List<String> keys = new ArrayList<String>();

        String docId = strLine.substring(0, strLine.indexOf(":") - 1);

        strLine = strLine.substring(strLine.indexOf(":") + 2);

        while (strLine.contains(",")) {
          keys.add(strLine.substring(0, strLine.indexOf(",")));
          strLine = strLine.substring(strLine.indexOf(",") + 1);
        }

        keys.add(strLine);

        Collections.reverse(keys);

        kpMap.put(docId, keys);
        br.close();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return kpMap;
  }

  /**
   * Print all info about keyphrases
   * 
   * @param kpMap
   * @param outputFile
   * @throws IOException
   */
  @Deprecated
  public static void printKeyphraseMaptoFile(Map<String, Keyphrase> kpMap,
      String outputFile) throws IOException {
    BufferedWriter out = null;

    // Create file
    File f = new File(outputFile);
    FileWriter fstream;
    try {
      fstream = new FileWriter(f);

      out = new BufferedWriter(fstream);

      Set<String> keySet = kpMap.keySet();

      for (String kpId : keySet) {
        Keyphrase keyphraseObj = kpMap.get(kpId);

        Integer emb = keyphraseObj.getEmbeddedness();
        Long freq = keyphraseObj.getFrequency();
        Long ybhits = keyphraseObj.getYbhits();
        Integer length = keyphraseObj.getLength();
        Double ts = keyphraseObj.getTaxonsSimilarity();

        out.write(kpId + "," + length + "," + freq + "," + ybhits + "," + emb
            + "," + ts + "\n");
      }

    } finally {
      out.close();
    }
  }

  /**
   * Print the document ids with the list of keyphrases and ranks
   * 
   * @param docMap
   */
  protected static void printDocMap(Map<String, Document> docMap) {
    Set<String> keySet = docMap.keySet();
    for (String docId : keySet) {
      System.out.print(docId + ":");
      Document doc = docMap.get(docId);
      Map<String, Double> keyphraseMap = doc.getKeyphraseRankMap();

      keyphraseMap = SaffronMapUtils.sortByValues(keyphraseMap);

      Set<String> keyphraseSet = keyphraseMap.keySet();

      for (String keyphrase : keyphraseSet) {
        // System.out.print(termextraction + " ( " + keyphraseMap.get(termextraction)
        // + " ),");

        System.out.print(keyphrase + ",");
      }

      System.out.println();
    }
  }

  public static Map<String, Keyphrase> readKPMap(File kpFile, ObjectMapper mapper) throws IOException {
      List<Keyphrase> data = mapper.readValue(kpFile, List.class);
      Map<String, Keyphrase> kpMap = new HashMap<String, Keyphrase>();
      for(Keyphrase kp : data) {
          kpMap.put(kp.getString(), kp);
      }
      return kpMap;
  }

}
