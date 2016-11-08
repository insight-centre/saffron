package org.insightcentre.nlp.saffron.documentindex;

import java.io.Closeable;
import org.insightcentre.nlp.saffron.data.Document;

public interface DocumentIndexer extends Closeable {
        public void indexDoc(Document id, String text) throws IndexingException;
		public void commit() throws IndexingException;
}
