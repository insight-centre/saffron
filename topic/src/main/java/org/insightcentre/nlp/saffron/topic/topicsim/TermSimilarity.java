package org.insightcentre.nlp.saffron.topic.topicsim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.TermSimilarityConfiguration;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TermSimilarity {

    private final double threshold;
    private final int topN;

    public TermSimilarity(TermSimilarityConfiguration config) {
        this.threshold = config.threshold;
        this.topN = config.topN;
    }

    public List<TermTerm> termSimilarity(List<DocumentTerm> documentTerms) {
        return termSimilarity(documentTerms, new DefaultSaffronListener());
    }
    
    public List<TermTerm> termSimilarity(List<DocumentTerm> documentTerms, SaffronListener log) {
        List<TermTerm> termTerms = new ArrayList<>();
        Map<String, Object2IntMap<String>> vectors = new HashMap<>();
        for (DocumentTerm dt : documentTerms) {
            if (!vectors.containsKey(dt.getTermString())) {
                vectors.put(dt.getTermString(), new Object2IntOpenHashMap<String>());
            }
            //if(dt.occurrences != null)
            vectors.get(dt.getTermString()).put(dt.getDocumentId(), dt.getOccurrences());
        }
        for (String t1 : vectors.keySet()) {
            TreeSet<TermTerm> topM = new TreeSet<>(new Comparator<TermTerm>() {

                @Override
                public int compare(TermTerm arg0, TermTerm arg1) {
                    int i1 = Double.compare(arg0.getSimilarity(), arg1.getSimilarity());
                    if (i1 == 0) {
                        int i2 = arg0.getTerm1().compareTo(arg1.getTerm1());
                        if (i2 == 0) {
                            return arg0.getTerm2().compareTo(arg1.getTerm2());
                        }
                        return i2;
                    }
                    return i1;
                }
            });
            for (String t2 : vectors.keySet()) {
                if (!t1.equals(t2)) {
                    double s = sim(vectors.get(t1), vectors.get(t2));
                    if (s > threshold) {
                        topM.add(new TermTerm(t1, t2, s));
                    }
                }
            }
            while (topM.size() > topN) {
                topM.pollFirst();
            }
            termTerms.addAll(topM);
        }

        return termTerms;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static TermSimilarity fromJsonString(String json) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, TermSimilarity.class);
    }


    private double sim(Object2IntMap<String> v1, Object2IntMap<String> v2) {
        double aa = 0, bb = 0, ab = 0;
        for (String s : v1.keySet()) {
            double a = v1.get(s);
            aa += a * a;
            if (v2.containsKey(s)) {
                ab += a * v2.get(s);
            }
        }
        for (String s : v2.keySet()) {
            double b = v2.get(s);
            bb += b * b;
        }
        if (aa == 0 || bb == 0) {
            return 0;
        }
        return ab / Math.sqrt(aa * bb);
    }
}
