package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * A corpus of documents for processing
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class IndexedCorpus implements Corpus {

    public final List<Document> documents;
    public final File index;


    /**
     * Create a corpus
     * @param documents The list of documents in the corpus
     * @param index The index where the documents are to be stored
     */
    @JsonCreator
    public IndexedCorpus(@JsonProperty("documents") List<Document> documents,
                  @JsonProperty("index") File index) {
        this.documents = documents;
        this.index = index;
    }

    public File getIndex() {
        return index;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void addDocument(Document document) {
        this.documents.add(document);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.documents);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IndexedCorpus other = (IndexedCorpus) obj;
        if (!Objects.equals(this.documents, other.documents)) {
            return false;
        }
        return true;
    }

    
}
