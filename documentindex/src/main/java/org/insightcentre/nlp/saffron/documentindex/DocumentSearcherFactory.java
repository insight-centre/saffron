package org.insightcentre.nlp.saffron.documentindex;

import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.documentindex.DocumentIndexFactory.LuceneAnalyzer;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneIndexer;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneSearcher;
import org.insightcentre.nlp.saffron.documentindex.tika.DocumentAnalyzer;

/**
 * For creating a document searcher
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class DocumentSearcherFactory {
    private DocumentSearcherFactory() {}
    /**
     * Create a document searcher
     * @param corpus The corpus metadata
     * @param index The location of the index
     * @return A document searcher
     * @throws IOException If a disk error occurs
     */
    public static DocumentSearcher loadSearcher(Corpus corpus, File index) throws IOException {
        return loadSearcher(corpus, index, false);
    }

    /**
     * Create a document searcher
     * @param corpus The corpus metadata
     * @param index The location of the index
     * @param rebuild Rebuild the corpus even if it already exists
     * @return A document searcher
     * @throws IOException If a disk error occurs
     */
 
    public static DocumentSearcher loadSearcher(Corpus corpus, File index, boolean rebuild) throws IOException {
        if (index == null) {
            throw new IllegalArgumentException("Corpus must have an index");
        } 
        
        final Directory dir;
        if (rebuild || !index.exists()) {
            dir = luceneFileDirectory(index, true);
            try(DocumentIndexer indexer = luceneIndexer(dir, LuceneAnalyzer.LOWERCASE_ONLY)) {
                for(Document doc : corpus.getDocuments()) {
                    Document doc2 = DocumentAnalyzer.analyze(doc);
                    indexer.indexDoc(doc2, doc2.getContents());
                }
                indexer.commit();
            }
        } else {
            dir = luceneFileDirectory(index, false);
        }
        return luceneSearcher(dir, LuceneAnalyzer.LOWERCASE_ONLY);
    }

    private static Directory luceneFileDirectory(File indexPath, boolean clearExistingIndex)
        throws IOException, IndexingException {
        if (clearExistingIndex) {
            FileUtils.deleteDirectory(indexPath);
        }
        return FSDirectory.open(indexPath);
    }

    private static DocumentIndexer luceneIndexer(Directory directory, LuceneAnalyzer analyzer)
        throws IndexingException {
        return new LuceneIndexer(directory, analyzer.analyzer);
    }

    private static DocumentSearcher luceneSearcher(Directory directory, LuceneAnalyzer analyzer)
        throws IOException {
        return new LuceneSearcher(directory, analyzer.analyzer);
    }

}
