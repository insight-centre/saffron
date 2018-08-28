package org.insightcentre.nlp.saffron.data.index;

import java.io.IOException;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;

/**
 * Treat a corpus as non-searchable document searcher
 * @author John McCrae
 */
public class CorpusAsDocumentSearcher implements DocumentSearcher {
    private final Corpus corpus;

    public CorpusAsDocumentSearcher(Corpus corpus) {
        this.corpus = corpus;
    }

    @Override
    public Iterable<Document> allDocuments() throws SearchException {
        return corpus.getDocuments();
    }

    @Override
    public Iterable<Document> search(String searchTerm) throws SearchException {
        throw new UnsupportedOperationException("Corpora is not indexed and cannot be searched");
    }

    @Override
    public void close() throws IOException {
    }
}
