package org.insightcentre.nlp.saffron.taxonomy.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StemUtils {
    public static String stemPhrase(String keyphrase) {
        // break each termextraction to get the keywords
        String[] keywords = keyphrase.split(" ");

        String stemmedKeyphrase = "";
        for (String keyword : keywords) {

            // check for words separated by '-'
            if (!keyword.contains("-")) {
                String stem = stemString(keyword);
                stemmedKeyphrase += stem + " ";
            } else {
                String[] words = keyword.split("-");

                for (String word : words) {
                    String stem = stemString(word);
                    stemmedKeyphrase += stem + "-";
                }

                // for words that do not end with '-' one more '-' was added at
                // the end, remove it
                if (keyword.endsWith("-")) {
                    stemmedKeyphrase = stemmedKeyphrase + " ";
                } else if (stemmedKeyphrase.endsWith("-")) {
                    stemmedKeyphrase =
                            stemmedKeyphrase.substring(0, stemmedKeyphrase.length() - 1)
                                    + " ";
                }
            }
        }
        if (stemmedKeyphrase.length() > 0) {
            return stemmedKeyphrase.substring(0, stemmedKeyphrase.length() - 1)
                    .toLowerCase();
        } else {
            return "";
        }
    }


    /**
     * The stemmer takes as input characters, not strings. This method stems a
     * string.
     *
     * @param word
     * @return
     */
    public static String stemString(String word) {
        Stemmer stemmer = new Stemmer();

        for (int i = 0; i < word.length(); i++) {
            stemmer.add(word.charAt(i));
        }
        stemmer.stem();

        String stem;
        stem = stemmer.toString();

        return stem;
    }

    /**
     * Drop-in replacement for LuceneUtils.stemAll
     */
    public static Map<String, String> stemAll(List<String> kpList) {
        Map<String, String> m = new HashMap<String, String>();
        for(String kp : kpList) {
            m.put(kp, stemPhrase(kp));
        }
        
        return m;
    }

}
