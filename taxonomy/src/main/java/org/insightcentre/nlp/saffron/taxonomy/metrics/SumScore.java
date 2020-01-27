package org.insightcentre.nlp.saffron.taxonomy.metrics;

import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.supervised.BinaryRelationClassifier;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 *
 * @author John McCrae
 */
public class SumScore implements Score<TypedLink> {

    private final BinaryRelationClassifier<String> classifier;
    private final Object2DoubleMap<TypedLink> scores = new Object2DoubleOpenHashMap<>();

    public SumScore(BinaryRelationClassifier<String> classifier) {
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
