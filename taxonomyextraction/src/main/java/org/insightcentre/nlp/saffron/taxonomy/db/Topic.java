package org.insightcentre.nlp.saffron.taxonomy.db;

import java.util.List;

public class Topic {

	private String preferredString, rootSequence;
	private Integer overallOccurrence, overallMatches;
	Double rank;
	private List<MorphologicalVariation> mvList;

	public Topic(String preferredString, String rootSequence, Integer overallOccurrence,
			Integer overallFrequency, Double rank, List<MorphologicalVariation> mvList) {
		super();
		this.preferredString = preferredString;
		this.rootSequence = rootSequence;
		this.overallOccurrence = overallOccurrence;
		this.overallMatches = overallFrequency;
		this.rank = rank;
		this.mvList = mvList;
	}

	public String getPreferredString() {
		return preferredString;
	}

	@Override
	public String toString() {
		return "Topic [preferredString=" + preferredString + ", rootSequence=" + rootSequence
				+ ", overallOccurrence=" + overallOccurrence + ", overallMatches="
				+ overallMatches + ", rank=" + rank + ", mvList=" + mvList + "]";
	}

	public String getRootSequence() {
		return rootSequence;
	}

	public Integer getOverallOccurrence() {
		return overallOccurrence;
	}

	public Integer getOverallMatches() {
		return overallMatches;
	}

	public Double getRank() {
		return rank;
	}

	public List<MorphologicalVariation> getMvList() {
		return mvList;
	}

}
