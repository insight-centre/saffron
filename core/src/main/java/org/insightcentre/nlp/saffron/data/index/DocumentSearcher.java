package org.insightcentre.nlp.saffron.data.index;

import java.io.Closeable;
import java.util.Collection;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;

public interface DocumentSearcher extends Corpus, Closeable {
    /**
     * Search document for a given term
     * @param searchTerm The term to search
     * @return An iterable covering all documents containing the term
     * @throws SearchException If an error occurs when searching
     */
    public Iterable<Document> search(String searchTerm) throws SearchException;

    /**
     * Replace a document in the current corpus
     * @param id The id of the document
     * @param doc The document to update
     */
    public void updateDocument(String id, Document doc);
    
    /**
     * Replace a set of Documents in the corpus
     * @param docs List of documents to update
     */
    public void updateDocuments(Collection<Document> docs);
}