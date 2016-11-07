package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A domain model (list of single words that are associated with a domain)
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class DomainModel {

    public final List<String> terms;

    @JsonCreator
    public DomainModel(@JsonProperty("terms") List<String> terms) {
        this.terms = terms;
    }

    public DomainModel() {
        this.terms = new ArrayList<>();
    }

    public List<String> getTerms() {
        return terms;
    }

    public void addTerm(String term) {
        terms.add(term);
    }

    public void removeTerm(String term) {
        terms.remove(term);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.terms);
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
        final DomainModel other = (DomainModel) obj;
        if (!Objects.equals(this.terms, other.terms)) {
            return false;
        }
        return true;
    }


}
