/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Georgeta Bordea
 * 
 */
@Deprecated public class TaxonUtils {

  public static final String INDEX_PATH = "output/lucene/index";
  
  public static final String KP_RANKS_FILE_NAME = "output/kpRanks.txt";
  public static final String KP_INFO_FILE_NAME = "output/kpInfo.txt";
  public static final String KP_TOKENS_SIM_FILE_NAME = "output/kpTokSim.txt";
  public static final String KP_DOCS_FILE_NAME = "output/kpDocsOutput.txt";
  public static final String KP_POST_RANKS_FILE_NAME = "output/kpPostRanks.txt";

  public static final Double SUBSUMPTION_COOCURRENCE_THRESHOLD = 0.7;

  public static final String NOUNS_FILE_NAME = "output/kpNouns.xml";
  public static final String VERBS_FILE_NAME = "output/kpVerbs.xml";
  public static final String ADJS_FILE_NAME = "output/kpAdjs.xml";

  public static final String NOUNS_OUTPUT_FILE_NAME = "rankedNouns.csv";
  public static final String VERBS_OUTPUT_FILE_NAME = "rankedVerbs.csv";
  public static final String ADJS_OUTPUT_FILE_NAME = "rankedAdjs.csv";

  public static final String BEST_100_KP_FILE_NAME = "best100KP.txt";
  public static final String BEST_200_KP_FILE_NAME = "best200KP.txt";
  public static final String BEST_500_KP_FILE_NAME = "best500KP.txt";
  public static final String BEST_100_VELARDI_FILE_NAME = "best100Velardi.txt";
  public static final String BEST_50_VELARDI_FILE_NAME = "best50Velardi.txt";
  public static final String BEST_1000_KP_FILE_NAME = "best1000KP.txt";
  public static final String DOCS_LENGTH_FILE_NAME = "docsLength.csv";

  public static final Long NOUNS_YBHITS_MAX = Long.parseLong("13300000000");
  public static final Integer TOKENS_NO = 652472;//21917865;
  public static final Integer DOCS_NO = 1999;//799;//1999;//2304;// 22;// 244;

  public static final Integer SPAN_SLOP =5;// 2;

  protected static List<String> readWordsFromFile(String path) {
    List<String> stopWords = new ArrayList<String>();

    try {
      BufferedReader input = new BufferedReader(new FileReader(path));
      try {
        String line = null;
        while ((line = input.readLine()) != null) {
          stopWords.add(line);
        }
      } finally {
        input.close();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    return stopWords;
  }
}
