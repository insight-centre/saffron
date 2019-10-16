package org.insightcentre.nlp.saffron.data.connections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * The link between a document and a topic
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentTopic {
    public final String document_id;
    public String term_string;
    public final int occurrences;
    public final String pattern;
    public final String acronym;
    //public final double score;
    public Double tfidf;
    //public Integer unembedded_occ;

    @JsonCreator
    public DocumentTopic(@JsonProperty(value="document_id",required=true) String document_id, 
                         @JsonProperty(value="term_string",required=true) String topic_string, 
                         @JsonProperty("occurences") int occurences, 
                         @JsonProperty("pattern") String pattern, 
                         @JsonProperty("acronym") String acronym,
                         @JsonProperty("tfidf") Double tfidf) {
        this.document_id = document_id;
        this.term_string = topic_string;
        this.occurrences = occurences;
        this.pattern = pattern;
        this.acronym = acronym;
        this.tfidf = tfidf;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.document_id);
        hash = 11 * hash + Objects.hashCode(this.term_string);
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
        if (!Objects.equals(this.term_string, other.term_string)) {
            return false;
        }
        return true;
    }

    

}
