package org.insightcentre.nlp.saffron.topic.tfidf;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.HashSet;
import java.util.List;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TFIDF {
    /**
     * Add the TF-IDF value to an existing set of unique document topic connections
     * @param docTerms The list of values to add TF-IDF scores to
     */
    public static void addTfidf(List<DocumentTerm> docTerms) {
        Object2DoubleMap<String> topicDf = new Object2DoubleOpenHashMap<>();
        HashSet<String> docNames = new HashSet<>();
        for(DocumentTerm dt : docTerms) {
            // We assume there are no duplicates in the DT list 
            topicDf.put(dt.getTermString(), topicDf.getDouble(dt.getTermString()) + 1.0);
            docNames.add(dt.getDocumentId());
        }
        double n = docNames.size();
        for(DocumentTerm dt : docTerms) {
            dt.setTfIdf((double) dt.getOccurrences() * Math.log(n / topicDf.getDouble(dt.getTermString())));
        }
    }
}
