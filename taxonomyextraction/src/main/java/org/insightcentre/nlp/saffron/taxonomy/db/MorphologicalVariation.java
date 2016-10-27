package org.insightcentre.nlp.saffron.taxonomy.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MorphologicalVariation {
	
	private final String string;
	
    @JsonCreator
	public MorphologicalVariation(@JsonProperty(value="string") String string) {
		super();
		this.string = string;
	}

	public String getString() {
		return string;
	}

	@Override
	public String toString() {
		return "MorphologicalVariation [string=" + string + "]";
	}
}
