package org.insightcentre.nlp.saffron.config;

/**
 * The configuration for topic similarity
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TopicSimilarityConfiguration {
    /** The minimum threshold for accepting similarity between two topics */
    public double threshold = 0.1;
    /** The maximum number of topics to accept */
    public int topN = 50;
}
