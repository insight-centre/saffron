package org.insightcentre.nlp.saffron.config;

/**
 * The configuration for author similarity extraction.
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class AuthorSimilarityConfiguration {
    /**
     * The minimum threshold of similarity to accept
     */
    public double threshold = 0.1;
    /**
     * The maximum number of similar authors (per author) to extract
     */
    public int topN = 50;
}
