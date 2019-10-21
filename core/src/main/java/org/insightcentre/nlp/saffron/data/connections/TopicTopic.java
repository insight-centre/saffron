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
public class TopicTopic {
    private String term1;
    private String term2;
    private final double similarity;
    private Status status;

    @JsonCreator
    public TopicTopic(@JsonProperty("term1_id") String term1, 
                      @JsonProperty("term2_id") String term2,
                      @JsonProperty("similarity") double similarity) {
        this.term1 = term1;
        this.term2 = term2;
        this.similarity = similarity;
    }

    public final static String JSON_TERM1_ID = "term1_id";
    @JsonProperty(JSON_TERM1_ID)
    public String getTerm1() {
        return term1;
    }

    public final static String JSON_TERM2_ID = "term2_id";
    @JsonProperty(JSON_TERM2_ID)
    public String getTerm2() {
        return term2;
    }

    public final static String JSON_SIMILARITY = "similarity";
    @JsonProperty(JSON_SIMILARITY)
    public double getSimilarity() {
        return similarity;
    }

    public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public void setTerm1(String term1) {
		this.term1 = term1;
	}

	public void setTerm2(String term2) {
		this.term2 = term2;
	}

	@Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.term1);
        hash = 89 * hash + Objects.hashCode(this.term2);
        hash = 89 * hash + (int) (Double.doubleToLongBits(this.similarity) ^ (Double.doubleToLongBits(this.similarity) >>> 32));
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
        final TopicTopic other = (TopicTopic) obj;
        if (!Objects.equals(this.term1, other.term1)) {
            return false;
        }
        if (!Objects.equals(this.term2, other.term2)) {
            return false;
        }
        if (Double.doubleToLongBits(this.similarity) != Double.doubleToLongBits(other.similarity)) {
            return false;
        }
        return true;
    }
    
}
