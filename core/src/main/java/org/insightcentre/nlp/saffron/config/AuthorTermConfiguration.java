package org.insightcentre.nlp.saffron.config;

/**
 * The configuration for the author-term similarity extraction
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class AuthorTermConfiguration {
    /** The maximum number of total author-term pairs to extract */
    public int topN = 1000;
    /** Exclude authors who are not authors of the minimum number of documents */
    public int minDocs = 1;
}
