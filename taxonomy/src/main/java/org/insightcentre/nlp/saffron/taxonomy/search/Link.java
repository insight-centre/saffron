package org.insightcentre.nlp.saffron.taxonomy.search;

public class Link {

	private String source;
	private String target;
	
	public Link(String source, String target) {
		this.source = source;
		this.target = target;
	}
	
	public String getSource() {
		return source;
	}
	public String getTarget() {
		return target;
	}
	@Override
	public String toString() {
		return "Link [source=" + source + ", target=" + target + "]";
	}
}
