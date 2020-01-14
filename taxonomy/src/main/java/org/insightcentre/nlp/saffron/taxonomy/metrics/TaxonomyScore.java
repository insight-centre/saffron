package org.insightcentre.nlp.saffron.taxonomy.metrics;

import java.util.Set;
import org.insightcentre.nlp.saffron.config.TaxonomySearchConfiguration;
import org.insightcentre.nlp.saffron.taxonomy.search.Link;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

/**
 * An interface for scorers for solutions. Each instance can only provide the
 * score for one change
 *
 * @author John McCrae
 */
public interface TaxonomyScore<T extends Link> {

    /**
     * Calculate the change in score of adding a single link
     *
     * @param taxoLink The taxonomy link to add
     * @return The change in score
     */
    double deltaScore(T taxoLink);

    /**
     * Generate a new taxonomy score for the solution which differs from this
     * solution only by adding the link bottom -&gt; top
     *
     * @param top The broader term
     * @param bottom The narrower term
     * @param soln The new solution
     * @return An object that is updated for this case
     */
    TaxonomyScore next(T link, Solution soln);

    /**
     * Create an instance of taxonomy search
     * @param config The configuration
     * @param score The value of the score (should be config.score)
     * @param classifier The classification function
     * @param terms The list of terms
     * @return A taxonomy scoring object initialized for the empty taxonomy
     */
    public static TaxonomyScore create(TaxonomySearchConfiguration config,
            TaxonomySearchConfiguration.Score score,
            SupervisedTaxo classifier, Set<String> terms) {
        if (null != score) {
            switch (score) {
                case simple:
                    return new SumScore(classifier);
                case transitive:
                    return new TransitiveScore(classifier);
                case bhattacharryaPoisson:
                    if (config.baseScore == TaxonomySearchConfiguration.Score.bhattacharryaPoisson) {
                        throw new IllegalArgumentException("Recursive score");
                    }
                    return new BhattacharryaPoisson(TaxonomyScore.create(config, config.baseScore, classifier, terms),
                            terms, config.aveChildren, config.alpha);
            }
        }
        throw new IllegalArgumentException("Unknown score");
    }
}
