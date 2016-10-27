package org.insightcentre.nlp.saffron.taxonomy.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Saffron2Paper {
	private final String id, text;

    @JsonCreator
	public Saffron2Paper(@JsonProperty(value="@id") String id, 
        @JsonProperty(value="text") String text) {
		super();
		this.id = id;
		this.text = text;
	}

    @JsonProperty(value="@id")
	public String getId() {
		return id;
	}

	public String getText() {
		return text;
	}
}
