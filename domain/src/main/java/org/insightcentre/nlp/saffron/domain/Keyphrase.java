package org.insightcentre.nlp.saffron.domain;

import java.util.Objects;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Keyphrase {
    public final String phrase;
    public final int length;

    public Keyphrase(String phrase, int length) {
        this.phrase = phrase;
        this.length = length;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.phrase);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Keyphrase other = (Keyphrase) obj;
        if (!Objects.equals(this.phrase, other.phrase)) {
            return false;
        }
        return true;
    }


    
}
