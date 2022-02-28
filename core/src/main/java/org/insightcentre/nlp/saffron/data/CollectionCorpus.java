package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Collections;

/**
 * A corpus in memory implemented with a Java collection
 * @author John McCrae
 */
public class CollectionCorpus implements Corpus {
    private final Collection<Document> corpus;

    public CollectionCorpus(@JsonProperty("documents") Collection<Document> corpus) {
        this.corpus = corpus;
    }

    @Override
    public Iterable<Document> getDocuments() {
        return Collections.unmodifiableCollection(corpus);
    }

    @Override
    public int size() {
        return corpus.size();
    }
    
    
}
