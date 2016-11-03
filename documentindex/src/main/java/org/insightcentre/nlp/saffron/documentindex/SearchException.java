package org.insightcentre.nlp.saffron.documentindex;

public class SearchException extends Exception {
	public SearchException(String message) {
		super(message);
	}

	public SearchException(String message, Exception e) {
		super(message, e);
	}
	
	private static final long serialVersionUID = -4243634570719820969L;

}
