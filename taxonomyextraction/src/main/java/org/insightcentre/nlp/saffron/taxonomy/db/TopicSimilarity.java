package org.insightcentre.nlp.saffron.taxonomy.db;

public class TopicSimilarity {

	public TopicSimilarity(Double similarityScore) {
		super();
		this.similarityScore = similarityScore;
	}

	public Double similarityScore;

	public Double getSimilarityScore() {
		return similarityScore;
	}

	@Override
	public String toString() {
		return "TopicSimilarity [similarityScore=" + similarityScore + "]";
	}

}
