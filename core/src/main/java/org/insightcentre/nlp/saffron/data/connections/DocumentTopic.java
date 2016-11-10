package org.insightcentre.nlp.saffron.data.connections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * The link between a document and a topic
 * @author John McCrae <john@mccr.ae>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentTopic {
    public final String document_id;
    public final String topic_string;
    public final int occurrences;
    public final String pattern;
    public final String acronym;
    public Double score;
    public Double tfidf;
    public Integer unembedded_occ;

    @JsonCreator
    public DocumentTopic(@JsonProperty(value="document_id",required=true) String document_id, 
                         @JsonProperty(value="topic_string",required=true) String topic_string, 
                         @JsonProperty("occurences") int occurences, 
                         @JsonProperty("pattern") String pattern, 
                         @JsonProperty("acronym") String acronym) {
        this.document_id = document_id;
        this.topic_string = topic_string;
        this.occurrences = occurences;
        this.pattern = pattern;
        this.acronym = acronym;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.document_id);
        hash = 11 * hash + Objects.hashCode(this.topic_string);
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
        final DocumentTopic other = (DocumentTopic) obj;
        if (!Objects.equals(this.document_id, other.document_id)) {
            return false;
        }
        if (!Objects.equals(this.topic_string, other.topic_string)) {
            return false;
        }
        return true;
    }

    

}
