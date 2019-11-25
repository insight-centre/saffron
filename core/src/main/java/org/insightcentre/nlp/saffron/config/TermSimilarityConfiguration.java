package org.insightcentre.nlp.saffron.config;

/**
 * The configuration for term similarity
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TermSimilarityConfiguration {
    /** The minimum threshold for accepting similarity between two terms */
    public double threshold = 0.1;
    /** The maximum number of terms to accept */
    public int topN = 50;
}
