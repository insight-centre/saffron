package org.insightcentre.nlp.saffron.taxonomy.search;

/**
 * A single link in a taxonomy
 * @author John McCrae
 */
public class TaxoLink extends TypedLink {

    public TaxoLink(String top, String bottom) {
    	super(top,bottom,TypedLink.Type.hypernymy);
    }
    
    public String getTop() {
    	return this.getSource();
    }
    
    public String getBottom() {
    	return this.getTarget();
    }

    @Override
    public String toString() {
        return "(" + getSource() + ", " + getTarget() + ')';
    }

}
