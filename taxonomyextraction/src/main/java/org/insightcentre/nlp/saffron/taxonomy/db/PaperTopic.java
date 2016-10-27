package org.insightcentre.nlp.saffron.taxonomy.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * A paper topic pairing
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class PaperTopic {
    public final String topicId;
    public final String paperId;

    @JsonCreator
    public PaperTopic(@JsonProperty("topicId") String topicId, 
        @JsonProperty("paperId") String paperId) {
        this.topicId = topicId;
        this.paperId = paperId;
    }

    public String getTopicId() {
        return topicId;
    }

    public String getPaperId() {
        return paperId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.topicId);
        hash = 97 * hash + Objects.hashCode(this.paperId);
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
        final PaperTopic other = (PaperTopic) obj;
        if (!Objects.equals(this.topicId, other.topicId)) {
            return false;
        }
        if (!Objects.equals(this.paperId, other.paperId)) {
            return false;
        }
        return true;
    }



}
