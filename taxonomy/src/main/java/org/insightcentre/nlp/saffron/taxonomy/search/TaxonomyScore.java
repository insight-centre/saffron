package org.insightcentre.nlp.saffron.taxonomy.search;

/**
 * An interface for scorers for solutions
 * 
 * @author John McCrae
 */
public interface TaxonomyScore {
    /**
     * The score for a given solution
     * @param solution The solution
     * @return 
     */
    double score(Solution solution);
}
