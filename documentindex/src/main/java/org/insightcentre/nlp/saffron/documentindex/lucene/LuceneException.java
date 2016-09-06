package org.insightcentre.nlp.saffron.documentindex.lucene;

public class LuceneException extends Exception {
	private static final long serialVersionUID = -677043193474928671L;

	public LuceneException(String message) {
        super(message);
    }

    public LuceneException(String message, Throwable cause) {
        super(message, cause);
    }
}
