package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The author of a document
 * 
 * @author John McCrae <john@mccr.ae>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Author {
    public final String name;
    public final Set<String> nameVariants;

    @JsonCreator
    public Author(@JsonProperty("name") String name,
                  @JsonProperty("name_variants") Set<String> nameVariants) {
        this.name = name;
        this.nameVariants = nameVariants;
    }

    public Author(String name) {
        this.name = name;
        this.nameVariants = new HashSet<String>();
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name_variants")
    public Set<String> getNameVariants() {
        return nameVariants;
    }

    

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
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
        final Author other = (Author) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    
}
