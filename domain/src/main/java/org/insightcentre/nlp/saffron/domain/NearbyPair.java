package org.insightcentre.nlp.saffron.domain;

import java.util.Objects;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class NearbyPair {
    public final String word;
    public final Keyphrase phrase;

    public NearbyPair(String word, Keyphrase phrase) {
        this.word = word;
        this.phrase = phrase;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.word);
        hash = 37 * hash + Objects.hashCode(this.phrase);
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
        final NearbyPair other = (NearbyPair) obj;
        if (!Objects.equals(this.word, other.word)) {
            return false;
        }
        if (!Objects.equals(this.phrase, other.phrase)) {
            return false;
        }
        return true;
    }
}
