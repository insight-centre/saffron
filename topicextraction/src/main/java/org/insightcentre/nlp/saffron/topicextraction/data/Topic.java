package org.insightcentre.nlp.saffron.topicextraction.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Topic {
    private String rootSequence;
    private Integer numberOfTokens;
    private List<MorphologicalVariation> mvs = new ArrayList<MorphologicalVariation>();
    
	public String toString() {
		return "Topic [rootSequence=" + rootSequence + ", numberOfTokens=" + numberOfTokens
				+ ", |morphologicalVariations|=" + mvs.size() + "]";
	}

	public Integer getNumberOfTokens() {
		return numberOfTokens;
	}

	public void setNumberOfTokens(Integer numberOfTokens) {
		this.numberOfTokens = numberOfTokens;
	}


	public List<MorphologicalVariation> getMorphologicalVariations() {
        return mvs;
    }

    public void addMorphologicalVariation(MorphologicalVariation mv) {
    	mvs.add(mv);
    }


	public void setMorphologicalVariations(List<MorphologicalVariation> mvs) {
        this.mvs = mvs;
    }
    
    public String getRootSequence() {
        return rootSequence;
    }

    public void setRootSequence(String rootSequence) {
        this.rootSequence = rootSequence;
    }

    
}
