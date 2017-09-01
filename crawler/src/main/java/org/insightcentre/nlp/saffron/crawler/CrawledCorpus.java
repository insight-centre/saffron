package org.insightcentre.nlp.saffron.crawler;

import java.util.List;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class CrawledCorpus implements Corpus {
    private final List<Document> documents;

    public CrawledCorpus(List<Document> documents) {
        this.documents = documents;
    }

    @Override
    public List<Document> getDocuments() {
        return documents;
    }
    
    
}
