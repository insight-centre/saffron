package org.insightcentre.nlp.saffron.taxonomy.metrics;

import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;

/**
 * A score that does not distinguish between any graphs. This is principally used 
 * for testing
 * 
 * @author John McCrae
 */
public class NullScore implements Score<TypedLink> {

    @Override
    public double deltaScore(TypedLink link) {
        return 0.0;
    }

    @Override
    public Score<TypedLink> next(TypedLink link, Solution soln) {
        return this;
    }

}
