package org.insightcentre.nlp.saffron.taxonomy.search;

/**
 * An interface for scorers for solutions. Each instance can only provide the 
 * score for one change
 * 
 * @author John McCrae
 */
public interface TaxonomyScore {
    /**
     * Calculate the chagne in score of adding a single link
     * @param taxoLink The taxonomy link to add
     * @return The change in score
     */
    double deltaScore(TaxoLink taxoLink);
    
    /**
     * Generate a new taxonomy score for the solution which differs from this 
     * solution only by adding the link bottom -> top
     * @param top The broader topic
     * @param bottom The narrower topic
     * @param soln The new solution
     * @return An object that is updated for this case
     */
    TaxonomyScore next(String top, String bottom, Solution soln);
}
