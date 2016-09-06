package org.insightcentre.nlp.saffron.taxonomy.db.saffron2;

public class Saffron2Paper {
	private String id, text;

	public Saffron2Paper(String id, String text) {
		super();
		this.id = id;
		this.text = text;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	
}
