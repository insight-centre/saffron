package org.insightcentre.nlp.saffron.data.connections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.insightcentre.nlp.saffron.data.Status;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TopicTopic {
    public String topic1;
    public String topic2;
    public final double similarity;
    public Status status;

    @JsonCreator
    public TopicTopic(@JsonProperty("topic1_id") String topic1, 
                      @JsonProperty("topic2_id") String topic2,
                      @JsonProperty("similarity") double similarity) {
        this.topic1 = topic1;
        this.topic2 = topic2;
        this.similarity = similarity;
    }

    @JsonProperty("topic1_id")
    public String getTopic1() {
        return topic1;
    }

    @JsonProperty("topic2_id")
    public String getTopic2() {
        return topic2;
    }

    @JsonProperty("similarity")
    public double getSimilarity() {
        return similarity;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.topic1);
        hash = 89 * hash + Objects.hashCode(this.topic2);
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
        if (!Objects.equals(this.topic1, other.topic1)) {
            return false;
        }
        if (!Objects.equals(this.topic2, other.topic2)) {
            return false;
        }
        if (Double.doubleToLongBits(this.similarity) != Double.doubleToLongBits(other.similarity)) {
            return false;
        }
        return true;
    }
    
}
