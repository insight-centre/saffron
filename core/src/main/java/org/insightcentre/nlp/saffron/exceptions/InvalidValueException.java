package org.insightcentre.nlp.saffron.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception used an invalid parameter value is passed to a Saffron method or object
 * 
 * @author Bianca Pereira
 *
 */
public class InvalidValueException extends RuntimeException{

	private static final long serialVersionUID = 5880455758949089736L;

	private Map<String,Object> parameterValue;
	
	public InvalidValueException(String message) {
		super(message);
		parameterValue = new HashMap<String,Object>();
	}
	
	public InvalidValueException(String message, Map<String,Object> parameterValueMap) {
		super(message);
		this.parameterValue = parameterValueMap;
	}

	public Map<String,Object> getParameterValueMap() {
		return parameterValue;
	}

	public void setValues(Map<String,Object> parameterValueMap) {
		this.parameterValue = parameterValueMap;
	}
	
	public void addParameterValue(String parameterName, Object value) {
		this.parameterValue.put(parameterName, value);
	}
}
