package org.insightcentre.nlp.saffron.topic.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The topic statistics step configuration
 * @author John McCrae <john@mccr.ae>
 */
public class Configuration {
    public double min_score = 1.7;

    @JsonProperty("min_score")
    public double getMinScore() {
        return min_score;
    }

    @JsonProperty("min_score")
    public void setMinScore(double min_score) {
        this.min_score = min_score;
    }

    

}
