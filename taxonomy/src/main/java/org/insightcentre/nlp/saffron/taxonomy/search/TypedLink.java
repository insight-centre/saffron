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
	
	public TypedLink(TypedLink toClone) {
		super(toClone.getSource(), toClone.getTarget());
		this.type = toClone.getType();
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!getClass().isInstance(obj))
			return false;
		TypedLink other = (TypedLink) obj;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TypedLink [type=" + type + ", source()=" + getSource() + ", target()=" + getTarget() + "]";
	}
	
}