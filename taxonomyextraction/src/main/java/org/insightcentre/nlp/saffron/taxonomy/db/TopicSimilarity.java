package org.insightcentre.nlp.saffron.taxonomy.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TopicSimilarity {

    @JsonCreator
	public TopicSimilarity(@JsonProperty(value="similarityScore", required = true) double similarityScore) {
		this.similarityScore = similarityScore;
	}

	public double similarityScore;

	public double getSimilarityScore() {
		return similarityScore;
	}

	@Override
	public String toString() {
		return "TopicSimilarity [similarityScore=" + similarityScore + "]";
	}

}
