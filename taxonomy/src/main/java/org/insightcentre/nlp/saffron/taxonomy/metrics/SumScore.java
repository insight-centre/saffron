package org.insightcentre.nlp.saffron.taxonomy.metrics;

import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.search.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 *
 * @author John McCrae
 */
public class SumScore implements Score<TypedLink> {

    private final SupervisedTaxo classifier;
    private final Object2DoubleMap<TypedLink> scores = new Object2DoubleOpenHashMap<>();

    public SumScore(SupervisedTaxo classifier) {
        this.classifier = classifier;
    }

    @Override
    public double deltaScore(TypedLink tl) {
        if (!scores.containsKey(tl)) {
            scores.put(tl, classifier.predict(tl.getSource(), tl.getTarget()));
        }
        return scores.getDouble(tl);
    }

    @Override
    public Score<TypedLink> next(TypedLink link, Solution soln) {
        return this;
    }
}
