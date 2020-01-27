package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.TaxoLink;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.taxonomy.metrics.Score;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * Implements a simple greedy search for the best taxonomy
 *
 * @author John McCrae
 */
public class Greedy implements TaxonomySearch {

    private final Score emptyScore;

    public Greedy(Score score) {
        this.emptyScore = score;
    }

    @Override
    public Taxonomy extractTaxonomyWithBlackWhiteList(Map<String, Term> termMap,
            Set<TaxoLink> whiteList, Set<TaxoLink> blackList) {
        Score score = this.emptyScore;
        ArrayList<TaxoLink> candidates = new ArrayList<>();
        if(termMap.size() == 0) {
            return new Taxonomy("NO TERMS", 0, 0, "", "", Collections.EMPTY_LIST, Status.none);
        } else if(termMap.size() == 1) {
            // It is not possible to construct a taxonomy from 1 term
            return new Taxonomy(termMap.keySet().iterator().next(), 0, 0, "", "", Collections.EMPTY_LIST, Status.none);
        }
        for (String t1 : termMap.keySet()) {
            for (String t2 : termMap.keySet()) {
                if (!t1.equals(t2)) {
                    candidates.add(new TaxoLink(t1, t2));
                }
            }
        }
        candidates.removeAll(blackList);
        candidates.removeAll(whiteList);

        TaxonomySolution soln = TaxonomySolution.empty(termMap.keySet());
        for (TaxoLink sp : whiteList) {
            if (termMap.get(sp.getTop()) != null && termMap.get(sp.getBottom()) != null) {
                soln = soln.add(sp.getTop(), sp.getBottom(),
                        termMap.get(sp.getTop()).getScore(),
                        termMap.get(sp.getBottom()).getScore(),
                        score.deltaScore(sp), true);
                score = score.next(sp, soln);
            }
        }
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
                TaxonomySolution soln2 = soln.add(candidate.getTop(), candidate.getBottom(),
                        termMap.get(candidate.getTop()).getScore(),
                        termMap.get(candidate.getBottom()).getScore(),
                        scores.getDouble(candidate), false);
                // soln2 = null means adding this link would create an invalid taxonomy
                if (soln2 != null) {
                    soln = soln2;
                    score = score.next(candidate, soln);
                    continue SOLN_LOOP;
                }
            }
            //System.err.println(soln);
            for (TaxoLink candidate : candidates) {
                System.err.println(candidate);
            }
            throw new RuntimeException("Failed to find solution");
        }
        return soln.toTaxonomy();
    }

}
