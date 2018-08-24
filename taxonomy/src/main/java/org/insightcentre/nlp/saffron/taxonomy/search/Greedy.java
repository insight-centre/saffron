package org.insightcentre.nlp.saffron.taxonomy.search;

import org.insightcentre.nlp.saffron.taxonomy.metrics.TaxonomyScore;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 * Implements a simple greedy search for the best taxonomy
 *
 * @author John McCrae
 */
public class Greedy implements TaxonomySearch{

    private final TaxonomyScore emptyScore;

    public Greedy(TaxonomyScore score) {
        this.emptyScore = score;
    }

    @Override
    public Taxonomy extractTaxonomy(Map<String, Topic> topicMap) {
        TaxonomyScore score = this.emptyScore;
        ArrayList<TaxoLink> candidates = new ArrayList<>();
        for (String t1 : topicMap.keySet()) {
            for (String t2 : topicMap.keySet()) {
                if (!t1.equals(t2)) {
                    candidates.add(new TaxoLink(t1, t2));
                }
            }
        }

        Solution soln = Solution.empty(topicMap.keySet());
        SOLN_LOOP:
        while (!soln.isComplete()) {
            final Object2DoubleMap<TaxoLink> scores = new Object2DoubleOpenHashMap<>();
            for (TaxoLink candidate : candidates) {
                scores.put(candidate, score.deltaScore(candidate));
            }
            candidates.sort(new Comparator<TaxoLink>() {
                @Override
                public int compare(TaxoLink o1, TaxoLink o2) {
                    double d1 = scores.getOrDefault(o1, Double.MIN_VALUE);
                    double d2 = scores.getOrDefault(o2, Double.MIN_VALUE);
                    int c = Double.compare(d1, d2);
                    return c == 0 ? o1.compareTo(o2) : -c;
                }
            });
            while (!candidates.isEmpty()) {
                TaxoLink candidate = candidates.remove(0);
                Solution soln2 = soln.add(candidate.top, candidate.bottom,
                        topicMap.get(candidate.top).score,
                        topicMap.get(candidate.bottom).score);
                // soln2 = null means adding this link would create an invalid taxonomy
                if (soln2 != null) {
                    soln = soln2;
                    score = score.next(candidate.top, candidate.bottom, soln);
                    continue SOLN_LOOP;
                }
            }
            throw new RuntimeException("Failed to find solution");
        }
        return soln.toTaxonomy();
    }

}
