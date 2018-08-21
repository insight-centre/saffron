package org.insightcentre.nlp.saffron.taxonomy.search;

/**
 * Compute the score for adding a single link to a solution, created by a Taxonomy Socre
 * 
 * @author John McCrae <john@mccr.ae>
 */
public interface TaxonomyLinkScore {
    /**
     * Calculate the chagne in score of adding a single link
     * @param taxoLink The taxonomy link to add
     * @return The change in score
     */
    double deltaScore(TaxoLink taxoLink);
    
}
