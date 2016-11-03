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
import org.insightcentre.nlp.saffron.documentindex.IndexingException;

public class LuceneIndexer implements DocumentIndexer, Closeable {

	private IndexWriter indexWriter;

	public LuceneIndexer(Directory directory, Analyzer analyzer) throws IndexingException {
		super();
		try {
			IndexWriterConfig config = new IndexWriterConfig(LuceneConfig.LUCENE_VERSION, analyzer);
			this.indexWriter = new IndexWriter(directory, config);
		} catch (IOException e) {
			throw new IndexingException(e.getMessage(), e);
		}
	}

	public void indexDoc(String id, String text) {
		try {
			indexWriter.addDocument(LuceneDocument.makeDocument(id, text));
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
