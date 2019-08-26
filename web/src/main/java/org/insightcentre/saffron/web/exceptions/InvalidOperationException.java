package org.insightcentre.saffron.web.exceptions;

/**
 * Exception used an invalid operation is requested
 * 
 * @author Bianca Pereira
 *
 */
public class InvalidOperationException extends RuntimeException{
	
	private static final long serialVersionUID = 7214764997089492228L;

	public InvalidOperationException(String message) {
		super(message);
	}	

}
