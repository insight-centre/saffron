/**
 * 
 */
package org.insightcentre.nlp.saffron.documentindex.lucene;


import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.insightcentre.nlp.saffron.documentindex.DocumentIndexer;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.documentindex.IndexingException;

public class LuceneIndexer implements DocumentIndexer, Closeable {

	private IndexWriter indexWriter;

	public LuceneIndexer(Directory directory, Analyzer analyzer) throws IndexingException {
		super();
		try {
			IndexWriterConfig config = new IndexWriterConfig(DocumentSearcherFactory.LUCENE_VERSION, analyzer);
			this.indexWriter = new IndexWriter(directory, config);
		} catch (IOException e) {
			throw new IndexingException(e.getMessage(), e);
		}
	}

    @Override
	public void indexDoc(org.insightcentre.nlp.saffron.data.Document doc, String text) {
		try {
            if(doc.id == null)
                throw new IllegalArgumentException("Error reading " + doc.file);
			indexWriter.addDocument(LuceneDocument.makeDocument(doc.id, text, doc.url, doc.authors, doc.name, doc.file.toFile(), doc.mimeType, doc.metadata));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void commit() {
		try {
			indexWriter.commit();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	@Override
	public void close() throws IOException {
		indexWriter.commit(); //Just in case one forgets!
		indexWriter.close();
	}
}
