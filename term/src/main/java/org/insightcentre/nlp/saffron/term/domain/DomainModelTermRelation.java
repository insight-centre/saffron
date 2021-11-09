package org.insightcentre.nlp.saffron.term.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that represents coocurrences between domain model terms and terms
 * 
 * @author Bianca Pereira
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainModelTermRelation {

	public final static String JSON_DOMAIN_TERM = "domain_model_term";
	@JsonProperty(JSON_DOMAIN_TERM)
	final private String domainTerm;
	
	public final static String JSON_TERM = "term";
	@JsonProperty(JSON_TERM)
	final private String term;
	
	public final static String JSON_FREQUENCY = "coocurrence_frequency";
	@JsonProperty(JSON_FREQUENCY)
	private int frequency;
	
	@JsonCreator
	public DomainModelTermRelation(
			@JsonProperty(value = JSON_DOMAIN_TERM) String domainTerm, 
			@JsonProperty(value = JSON_TERM) String term,
			@JsonProperty(value = JSON_FREQUENCY) int frequency) {
		this.domainTerm = domainTerm;
		this.term = term;
		this.frequency = frequency;
	}
	
	public String getDomainTerm() {
		return domainTerm;
	}

	public String getTerm() {
		return term;
	}

	public int getFrequency() {
		return frequency;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domainTerm == null) ? 0 : domainTerm.hashCode());
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DomainModelTermRelation other = (DomainModelTermRelation) obj;
		if (domainTerm == null) {
			if (other.domainTerm != null)
				return false;
		} else if (!domainTerm.equals(other.domainTerm))
			return false;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DomainModelTermRelation [domainTerm=" + domainTerm + ", term=" + term + ", frequency=" + frequency + "]";
	}
}
