package org.insightcentre.nlp.saffron.data.connections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.insightcentre.nlp.saffron.data.Status;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorAuthor {
    public final String author1_id;
    public final String author2_id;
    public final double similarity;
    public Status status;

    @JsonCreator
    public AuthorAuthor(@JsonProperty("author1_id") String author1_id, 
                        @JsonProperty("author2_id") String author2_id, 
                        @JsonProperty("similarity") double similarity) {
        this.author1_id = author1_id;
        this.author2_id = author2_id;
        this.similarity = similarity;
    }

    public String getAuthor1_id() {
        return author1_id;
    }

    public String getAuthor2_id() {
        return author2_id;
    }

    public double getSimilarity() {
        return similarity;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.author1_id);
        hash = 71 * hash + Objects.hashCode(this.author2_id);
        hash = 71 * hash + (int) (Double.doubleToLongBits(this.similarity) ^ (Double.doubleToLongBits(this.similarity) >>> 32));
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
        final AuthorAuthor other = (AuthorAuthor) obj;
        if (!Objects.equals(this.author1_id, other.author1_id)) {
            return false;
        }
        if (!Objects.equals(this.author2_id, other.author2_id)) {
            return false;
        }
        if (Double.doubleToLongBits(this.similarity) != Double.doubleToLongBits(other.similarity)) {
            return false;
        }
        return true;
    }

    
    
}
