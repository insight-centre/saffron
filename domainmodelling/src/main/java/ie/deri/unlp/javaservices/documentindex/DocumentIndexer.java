package ie.deri.unlp.javaservices.documentindex;

import java.io.Closeable;

public interface DocumentIndexer extends Closeable {
        public void indexDoc(String id, String text) throws IndexingException;
		public void commit() throws IndexingException;
}
