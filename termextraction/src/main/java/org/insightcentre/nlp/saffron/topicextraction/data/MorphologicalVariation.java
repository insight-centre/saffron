package org.insightcentre.nlp.saffron.topicextraction.data;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class MorphologicalVariation {
    private String termString;
    @XmlTransient
    private String pattern;

    /**
     * Number of times a term is found by the topic extractor.
     * Note that this can be greater than documentOccurrences because
     * more than one pattern-matching rule may match the same topic. 
     */
    private Integer extractedTermOccurrences;
    private String acronym;
    private String expandedAcronym;

    public MorphologicalVariation() {

    }

	public String getTermString() {
		return termString;
	}

	public void setTermString(String termString) {
		this.termString = termString;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}


	public Integer getExtractedTermOccurrences() {
		return extractedTermOccurrences;
	}

	public void setExtractedTermOccurrences(Integer extractedTermOccurrences) {
		this.extractedTermOccurrences = extractedTermOccurrences;
	}

	@Override
	public String toString() {
		return "MorphologicalVariation [termString=" + termString + ", pattern=" + pattern
				+ ", extractedTermOccurrences=" + extractedTermOccurrences + 
				", acronym=" + acronym +
				", expandedAcronym=" + expandedAcronym + "]";
	}

	public String getAcronym() {
		return acronym;
	}

	public void setAcronym(String acronym) {
		this.acronym = acronym;
	}

	public String getExpandedAcronym() {
		return expandedAcronym;
	}

	public void setExpandedAcronym(String expandedAcronym) {
		this.expandedAcronym = expandedAcronym;
	}
    
    


}
