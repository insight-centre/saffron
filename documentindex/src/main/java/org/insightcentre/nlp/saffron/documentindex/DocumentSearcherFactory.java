package org.insightcentre.nlp.saffron.documentindex;

import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ListIterator;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneIndexer;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneSearcher;
import org.insightcentre.nlp.saffron.documentindex.tika.DocumentAnalyzer;

/**
 * For creating a document searcher
 *
 * @author John McCrae <john@mccr.ae>
 */
public class DocumentSearcherFactory {
    
    private DocumentSearcherFactory() {
    }

    /**
     * Create a document searcher
     *
     * @param corpus The corpus metadata
     * @param index The location of the index
     * @return A document searcher
     * @throws IOException If a disk error occurs
     */
    public static DocumentSearcher loadSearcher(Corpus corpus, File index) throws IOException {
        return loadSearcher(corpus, index, false);
    }
    
    private static final DocumentAnalyzer TIKA_ANALYZER = new DocumentAnalyzer();

    /**
     * Create a document searcher
     *
     * @param corpus The corpus metadata
     * @param index The location to build the index
     * @param rebuild Rebuild the corpus even if it already exists
     * @return A document searcher
     * @throws IOException If a disk error occurs
     */
    // This is called by reflection in DocumentSearchFactory so do not change the signature!
    public static DocumentSearcher loadSearcher(Corpus corpus, File index, Boolean rebuild) throws IOException {
        final Directory dir;
        if (corpus instanceof IndexedCorpus) {
            IndexedCorpus ic = (IndexedCorpus) corpus;
            if (ic.index != null && !ic.index.toFile().equals(index)) {
                System.err.println("Corpus location does not match supplied index");
            }
        }
        if (corpus instanceof IndexedCorpus && index.exists() && !rebuild) {
            dir = luceneFileDirectory(index, false);
            IndexedCorpus ic = (IndexedCorpus) corpus;
            LuceneSearcher searcher = new LuceneSearcher(dir, LOWERCASE_ONLY);
            ListIterator<Document> docs = ic.documents.listIterator();
            while (docs.hasNext()) {
                docs.set(docs.next().withLoader(searcher));
            }
        } else {
            if (index == null) {
                throw new IllegalArgumentException("Corpus must have an index");
            }
            dir = luceneFileDirectory(index, true);
            try (DocumentIndexer indexer = luceneIndexer(dir, LOWERCASE_ONLY)) {
                for (Document doc : corpus.getDocuments()) {
                    Document doc2 = TIKA_ANALYZER.analyze(doc);
                    indexer.indexDoc(doc2, doc2.contents());
                }
                indexer.commit();
            }
            
        }
        return luceneSearcher(dir, LOWERCASE_ONLY);
    }
    
    private static Directory luceneFileDirectory(File indexPath, boolean clearExistingIndex)
            throws IOException, IndexingException {
        if (clearExistingIndex) {
            FileUtils.deleteDirectory(indexPath);
        }
        return FSDirectory.open(indexPath);
    }
    
    private static DocumentIndexer luceneIndexer(Directory directory, Analyzer analyzer)
            throws IndexingException {
        return new LuceneIndexer(directory, analyzer);
    }
    
    private static DocumentSearcher luceneSearcher(Directory directory, Analyzer analyzer)
            throws IOException {
        return new LuceneSearcher(directory, analyzer);
    }
    
    private static final Analyzer LOWERCASE_ONLY = new AnalyzerLower();
    
    public static final Version LUCENE_VERSION = Version.LUCENE_44;

    public static final class AnalyzerLower extends Analyzer {
        
        @Override
        protected Analyzer.TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
            StandardTokenizer source = new StandardTokenizer(LUCENE_VERSION, reader);
            
            TokenStream filter = new StandardFilter(LUCENE_VERSION, source);
            filter = new LowerCaseFilter(LUCENE_VERSION, filter);
            
            return new Analyzer.TokenStreamComponents(source, filter);
        }
    }
}
