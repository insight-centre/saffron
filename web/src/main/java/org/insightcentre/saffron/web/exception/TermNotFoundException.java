package org.insightcentre.saffron.web.exception;

import org.insightcentre.nlp.saffron.data.Term;

/**
 * Exception to be used when a {@link Term} is requested but it is not found
 * 
 * @author Bianca Pereira
 *
 */
public class TermNotFoundException extends Exception {

	private static final long serialVersionUID = -4991810975335508654L;
	private Term term;
	
	public TermNotFoundException(String termString) {
		super("Term not found");
		this.term = new Term.Builder(termString).build();
	}
	
	public TermNotFoundException(Term term) {
		super("Term not found");
		this.term = term;
	}
	
	public TermNotFoundException(String message, String termString) {
		super(message);
		this.term = new Term.Builder(termString).build();
	}
	
	public TermNotFoundException(String message, Term term) {
		super(message);
		this.term = term;
	}
	
	public Term getTerm() {
		return this.term;
	}
}
