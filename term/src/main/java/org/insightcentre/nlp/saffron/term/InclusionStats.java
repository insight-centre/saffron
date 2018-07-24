package org.insightcentre.nlp.saffron.term;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.insightcentre.nlp.saffron.term.TermExtractionTask.join;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class InclusionStats {
    public final Map<String, Set<String>> subTerms = new HashMap<>();
    public final Object2IntMap<String> superTerms = new Object2IntOpenHashMap<>();
    
    public InclusionStats(Object2IntMap<String> freqs) {
        calcInclusions(freqs);
    }

    private void calcInclusions(Object2IntMap<String> freqs) {
        for(String term : freqs.keySet()) {
            String[] tokens = term.split(" ");
            for(int i = 0; i < tokens.length; i++) {
                for(int j = i + 1; j <= tokens.length; j++) {
                    String t = join(tokens, i, j+1);
                    if(freqs.containsKey(t)) {
                        if(!subTerms.containsKey(term)) {
                            subTerms.put(term, new HashSet<String>());
                        }
                        subTerms.get(term).add(t);
//                        if(!superTerms.containsKey(t)) {
//                            superTerms.put(term, new HashSet<String>());
//                        }
//                        superTerms.get(t).add(term);
                        superTerms.put(t, superTerms.getInt(t) + 1);
                    } 
                }
            }
        }
    }
}
