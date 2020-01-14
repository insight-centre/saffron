package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.Objects;

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
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.getSource());
        hash = 17 * hash + Objects.hashCode(this.getTarget());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TaxoLink other = (TaxoLink) obj;
        if (!Objects.equals(this.getSource(), other.getSource())) {
            return false;
        }
        if (!Objects.equals(this.getTarget(), other.getTarget())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "(" + getSource() + ", " + getTarget() + ')';
    }

}
