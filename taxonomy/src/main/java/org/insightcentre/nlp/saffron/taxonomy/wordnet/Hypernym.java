package org.insightcentre.nlp.saffron.taxonomy.wordnet;

import java.util.Objects;

/**
 *
 * @author John McCrae
 */
public class Hypernym {

    public String hyponym, hypernym;

    public Hypernym() {
    }

    public Hypernym(String hyponym, String hypernym) {
        this.hyponym = hyponym;
        this.hypernym = hypernym;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.hyponym);
        hash = 13 * hash + Objects.hashCode(this.hypernym);
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
        final Hypernym other = (Hypernym) obj;
        if (!Objects.equals(this.hyponym, other.hyponym)) {
            return false;
        }
        if (!Objects.equals(this.hypernym, other.hypernym)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Hypernym{" + "hyponym=" + hyponym + ", hypernym=" + hypernym + '}';
    }

}
