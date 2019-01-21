package org.insightcentre.nlp.saffron.documentindex;

import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneIndexer;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneSearcher;

/**
 * For creating a document searcher
 *
 * @author John McCrae <john@mccr.ae>
 */
public class DocumentSearcherFactory {

    private DocumentSearcherFactory() {
    }

    /**
     * Load a searcher from a Lucene Index
     *
     * @param index The path to the index
     * @return The searcher
     * @throws IOException If an IO Exception occurs
     */
    public static DocumentSearcher load(File index) throws IOException {
        Directory dir = luceneFileDirectory(index, false);
        return luceneSearcher(dir, LOWERCASE_ONLY);
    }

    /**
     * Index a corpus
     *
     * @param corpus The corpus metadata
     * @param index The location of the index
     * @return A document searcher
     * @throws IOException If a disk error occurs
     */
    public static DocumentSearcher index(Corpus corpus, File index) throws IOException {
        return index(corpus, index, new DefaultSaffronListener());
    }
    
    public static DocumentSearcher index(Corpus corpus, File index, SaffronListener log) throws IOException {
        final Directory dir = luceneFileDirectory(index, true);
        try (DocumentIndexer indexer = luceneIndexer(dir, LOWERCASE_ONLY)) {
            log.log("Indexing");
            for (Document doc : corpus.getDocuments()) {
                log.log(doc.name);
                //Document doc2 = TIKA_ANALYZER.analyze(doc);
                indexer.indexDoc(doc, doc.contents());
            }
            indexer.commit();
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
