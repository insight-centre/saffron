package org.insightcentre.nlp.saffron.taxonomy.search;

import org.insightcentre.nlp.saffron.data.Ontonomy;
import org.insightcentre.nlp.saffron.data.TypedLink;

public class OntonomySolution extends Solution{

	private Ontonomy ontonomy; 
	
	public OntonomySolution() {
		this.ontonomy = new Ontonomy();
	}
	
	public OntonomySolution(Ontonomy ontonomy) {
		this.ontonomy = ontonomy;
	}
	
	/**
     * Create a new empty solution
     *
     * @return An empty solution
     */
    public static OntonomySolution empty() {
        return new OntonomySolution();
    }
    
    /**
     * Add a link to create a new partial solution
     *
     * @param link The link to be added to the solution
     * @param sourceScore The score of the source term
     * @param targetScore The score of the target term
     * @param linkScore The link score
     * @param required Is this a required (inclusion list) term
     * @return
     */
	public OntonomySolution add(TypedLink link, double sourceScore, double targetScore, double linkScore,
			boolean required) {
		return new OntonomySolution(new Ontonomy(this.ontonomy, link));
	}

    /**
     * Convert this to an ontonomy
     *
     * @return The complete ontonomy
     */
    public Ontonomy toOntonomy() {
        return ontonomy;
    }


	
}
