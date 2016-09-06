package org.insightcentre.nlp.saffron.taxonomy.db;

public class MorphologicalVariation {
	
	private String string;
	
	public MorphologicalVariation(String string) {
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
