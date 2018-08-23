package org.insightcentre.nlp.saffron.taxonomy.metrics;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxoLink;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

/**
 *
 * @author John McCrae
 */
public class TransitiveScore implements TaxonomyScore {

    private final SupervisedTaxo classifier;
    private final Object2DoubleMap<TaxoLink> scores;
    private final HashMap<String, Set<String>> parents;

    public TransitiveScore(SupervisedTaxo classifier) {
        this.classifier = classifier;
        this.parents = new HashMap<>();
        this.scores = new Object2DoubleOpenHashMap<>();
    }

    private TransitiveScore(SupervisedTaxo classifier, Object2DoubleMap<TaxoLink> scores, HashMap<String, Set<String>> parents) {
        this.classifier = classifier;
        this.scores = scores;
        this.parents = parents;
    }

    @Override
    public double deltaScore(TaxoLink tl) {
        if (!scores.containsKey(tl)) {
            scores.put(tl, classifier.predict(tl.top, tl.bottom));
        }
        double s = scores.getDouble(tl);
        if(parents.containsKey(tl.top)) {
            for(String p : parents.get(tl.top)) {
                TaxoLink tl2 = new TaxoLink(p, tl.top);
                if (!scores.containsKey(tl2)) {
                    scores.put(tl2, classifier.predict(tl.top, tl.bottom));
                }
                s += scores.getDouble(tl2);
            }
        }
        return s;
    }

    @Override
    public TaxonomyScore next(String top, String bottom, Solution soln) {
        HashMap<String, Set<String>> newParents = new HashMap<>(parents);
        Set<String> p = new HashSet<>();
        if(newParents.containsKey(top)) {
            p.addAll(newParents.get(top));
        }
        p.add(top);
        newParents.put(bottom, p);
        return new TransitiveScore(classifier, scores, newParents);
    }

}
