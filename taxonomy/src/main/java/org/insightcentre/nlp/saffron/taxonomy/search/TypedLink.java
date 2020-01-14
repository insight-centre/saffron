package org.insightcentre.nlp.saffron.taxonomy.search;

public class TypedLink extends Link implements Comparable<TypedLink>{

	public enum Type {
		hypernymy, hyponymy, meronymy, synonymy, other
	}
	
	private Type type;
	 
	public TypedLink(String source, String target, Type type) {
		super(source, target);
		this.type = type;
	} 

	public TypedLink(String source, String target, String type) {
		super(source, target);
		this.type = Type.valueOf(type);
	}

	public Type getType() {
		return type;
	}

	@Override
	public int compareTo(TypedLink o) {
		int c0 = this.getType().compareTo(o.getType());
        if (c0 != 0) {
            return c0;
        }
			
		int c1 = this.getSource().compareTo(o.getSource());
        if (c1 != 0) {
            return c1;
        }
        int c2 = this.getTarget().compareTo(o.getTarget());
        return c2;
	}
	
}