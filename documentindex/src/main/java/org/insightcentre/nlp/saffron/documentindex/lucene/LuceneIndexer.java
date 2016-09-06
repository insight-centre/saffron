/**
 * 
 */
package org.insightcentre.nlp.saffron.documentindex.lucene;

import org.insightcenter.nlp.saffron.documentindex.DocumentIndexer;
import org.insightcenter.nlp.saffron.documentindex.IndexingException;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

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

	public void indexDoc(String id, String text) throws IndexingException {
		try {
			indexWriter.addDocument(LuceneDocument.makeDocument(id, text));
		} catch (IOException e) {
			throw new IndexingException(e.getMessage(), e);
		}
	}

	@Override
	public void commit() throws IndexingException {
		try {
			indexWriter.commit();
		} catch (IOException e) {
			throw new IndexingException(e.getMessage(), e);
		}
	}
	
	@Override
	public void close() throws IOException {
		indexWriter.commit(); //Just in case one forgets!
		indexWriter.close();
	}
}
