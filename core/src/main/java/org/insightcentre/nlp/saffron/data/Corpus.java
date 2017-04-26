package org.insightcentre.nlp.saffron.data;

/**
 * A corpus
 * 
 * @author John McCrae <john@mccr.ae>
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
    
}
