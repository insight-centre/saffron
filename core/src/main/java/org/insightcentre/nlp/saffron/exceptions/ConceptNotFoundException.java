package org.insightcentre.nlp.saffron.exceptions;

import org.insightcentre.nlp.saffron.data.Concept;

/**
 * Exception to be used when a {@link Concept} is requested but it is not found
 * 
 * @author Bianca Pereira
 *
 */
public class ConceptNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -4991810975335508654L;
	private Concept concept;
	
	public ConceptNotFoundException(Concept concept) {
		super("Concept not found");
		this.concept = concept;
	}
	
	public ConceptNotFoundException(String message, Concept concept) {
		super(message);
		this.concept = concept;
	}
	
	public Concept getConcept() {
		return this.concept;
	}
}
