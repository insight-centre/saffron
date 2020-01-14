package org.insightcentre.nlp.saffron.taxonomy.metrics;

import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.search.TypedLink;

public interface Score<T extends TypedLink>{

	/**
     * Calculate the change in score of adding a single link
     *
     * @param taxoLink The taxonomy link to add
     * @return The change in score
     */
    double deltaScore(T link);

    /**
     * Generate a new taxonomy score for the solution which differs from this
     * solution only by adding the link bottom -&gt; top
     *
     * @param top The broader term
     * @param bottom The narrower term
     * @param soln The new solution
     * @return An object that is updated for this case
     */
    Score<T> next(T link, Solution soln);
}
