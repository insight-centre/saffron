package org.insightcentre.nlp.saffron.documentindex.lucene;

import java.io.File;
import org.insightcentre.nlp.saffron.data.index.SearchException;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;

/**
 * @author Georgeta Bordea
 *
 */
public class LuceneSearcher implements DocumentSearcher {

    private final IndexSearcher occurrence_searcher;

    public LuceneSearcher(Directory directory, Analyzer analyzer) throws IOException {
        DirectoryReader reader = DirectoryReader.open(directory);
        occurrence_searcher = new IndexSearcher(reader);

    }

    @Override
    public void close() throws IOException {
        occurrence_searcher.getIndexReader().close();
    }

    private static class AllDocIterator implements Iterator<org.insightcentre.nlp.saffron.data.Document> {

        final IndexReader reader;
        org.insightcentre.nlp.saffron.data.Document data;
        int i;

        public AllDocIterator(IndexReader reader) {
            this.reader = reader;
            this.data = null;
            this.i = 0;
            advance();
        }

        private void advance() {
            while (i < reader.maxDoc()) {
                Document d;
                try {
                    d = reader.document(i);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                if (d != null) {
                    data = new org.insightcentre.nlp.saffron.data.Document(new File(d.get(LuceneDocument.SOURCE_FILE)),
                            d.get(LuceneDocument.UID_NAME),
                            d.get(LuceneDocument.FULL_NAME),
                            d.get(LuceneDocument.MIME_TYPE),
                            LuceneDocument.unmkAuthors(d.get(LuceneDocument.AUTHORS_NAME)),
                            LuceneDocument.unmkMetadata(d.get(LuceneDocument.METADATA))
                    );
                    data.setContents(d.get(LuceneDocument.CONTENTS_NAME));
                    i++;
                    return;
                }
                i++;
            }
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean hasNext() {
            return i < reader.maxDoc();
        }

        @Override
        public org.insightcentre.nlp.saffron.data.Document next() {
            org.insightcentre.nlp.saffron.data.Document d = data;
            advance();
            return d;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }

    }

    @Override
    public Iterable<org.insightcentre.nlp.saffron.data.Document> allDocuments() throws SearchException {
        return new Iterable<org.insightcentre.nlp.saffron.data.Document>() {

            @Override
            public Iterator<org.insightcentre.nlp.saffron.data.Document> iterator() {
                return new AllDocIterator(occurrence_searcher.getIndexReader());
            }
        };
    }

}
