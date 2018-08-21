package org.insightcentre.nlp.saffron.taxonomy.search;

/**
 * An interface for scorers for solutions
 * 
 * @author John McCrae
 */
public interface TaxonomyScore {
    /**
     * The link scorer for a given solution
     * @param solution The solution
     * @return The object that can calculate the change in score
     */
    TaxonomyLinkScore score(Solution solution);
}
