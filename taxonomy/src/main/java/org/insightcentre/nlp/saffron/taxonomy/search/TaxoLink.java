package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.Objects;

/**
 * A single link in a taxonomy
 * @author John McCrae
 */
public class TaxoLink implements Comparable<TaxoLink> {

    public final String top;
    public final String bottom;

    public TaxoLink(String _s1, String _s2) {
        this.top = _s1;
        this.bottom = _s2;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.top);
        hash = 17 * hash + Objects.hashCode(this.bottom);
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
        if (!Objects.equals(this.top, other.top)) {
            return false;
        }
        if (!Objects.equals(this.bottom, other.bottom)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "(" + top + ", " + bottom + ')';
    }

    @Override
    public int compareTo(TaxoLink o) {
        int c1 = this.top.compareTo(o.top);
        if (c1 != 0) {
            return c1;
        }
        int c2 = this.bottom.compareTo(o.bottom);
        return c2;
    }

}
