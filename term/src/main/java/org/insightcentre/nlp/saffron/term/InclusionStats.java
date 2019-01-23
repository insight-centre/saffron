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
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class InclusionStats {

    public final Map<String, Set<String>> superTerms = new HashMap<>();
    public final Object2IntMap<String> subTerms = new Object2IntOpenHashMap<>();

    public InclusionStats(Object2IntMap<String> freqs) {
        calcInclusions(freqs);
    }

    private void calcInclusions(Object2IntMap<String> freqs) {
        for (String term : freqs.keySet()) {
            String[] tokens = term.split(" ");
            for (int i = 0; i < tokens.length; i++) {
                for (int j = i + 1; j <= tokens.length; j++) {
                    if (j - i != tokens.length) {
                        String t = join(tokens, i, j - 1);
                        if (freqs.containsKey(t)) {
                            if (!superTerms.containsKey(t)) {
                                superTerms.put(t, new HashSet<String>());
                            }
                            subTerms.put(term, subTerms.getInt(term) + 1);
                            superTerms.get(t).add(term);
                        }
                    }
                }
            }
        }
    }
}
