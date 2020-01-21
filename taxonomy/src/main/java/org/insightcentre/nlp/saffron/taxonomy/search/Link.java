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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!getClass().isInstance(obj))
			return false;
		Link other = (Link) obj;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Link [source=" + source + ", target=" + target + "]";
	}
}
