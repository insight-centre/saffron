package org.insightcenter.nlp.saffron.documentindex;

import java.io.Closeable;

public interface DocumentIndexer extends Closeable {
        public void indexDoc(String id, String text) throws IndexingException;
		public void commit() throws IndexingException;
}
