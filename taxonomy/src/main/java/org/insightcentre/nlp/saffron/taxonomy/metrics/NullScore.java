package org.insightcentre.nlp.saffron.taxonomy.metrics;

import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxoLink;

/**
 * A score that does not distinguish between any trees. Thsi is principally used 
 * for testing
 * 
 * @author John McCrae
 */
public class NullScore implements TaxonomyScore<TaxoLink> {

    @Override
    public double deltaScore(TaxoLink taxoLink) {
        return 0.0;
    }

    @Override
    public TaxonomyScore next(TaxoLink link, Solution soln) {
        return this;
    }

}
