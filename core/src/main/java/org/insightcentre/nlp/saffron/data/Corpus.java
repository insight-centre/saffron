package org.insightcentre.nlp.saffron.data;

/**
 * A corpus
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public interface Corpus {
    /**
     * Get all the documents in this corpus. This is an iterator so we assume
     * that documents should not be cached and some implementations may clean up
     * document between calls to next()
     * 
     * @return The documents in no particular order
     */
    public Iterable<Document> getDocuments();
    
    
    /**
     * The number of documents in this corpus
     * @return The size of this corpus
     */
    public int size();
}
