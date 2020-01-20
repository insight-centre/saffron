package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.taxonomy.search.testing.KnowledgeGraph;

public class KnowledgeGraphSolution extends Solution{

	private TaxonomySolution taxonomy;
	private TaxonomySolution partonomy;
    public final Set<String> terms;
    
    //private Set<TypedLink> visitedLinks;
    //private long size;
    //what about synonyms?
	
    private KnowledgeGraphSolution(Set<String> terms) {
        this.taxonomy = TaxonomySolution.empty(terms);
        this.partonomy = TaxonomySolution.empty(terms);
        this.taxonomy = TaxonomySolution.empty(terms);
        this.terms = terms;
    }
    
    public KnowledgeGraph getKnowledgeGraph() {
    	KnowledgeGraph kg = new KnowledgeGraph();
    	kg.setTaxonomy(this.getTaxonomy());
    	kg.setPartonomy(this.getPartonomy());
    	return kg;
    }
    
    private Taxonomy getTaxonomy() {
    	return this.taxonomy.toTaxonomy();
    }
    
    private List<Taxonomy> getPartonomy(){
    	List<Taxonomy> partonomy = new ArrayList<Taxonomy>();
    	for(Taxonomy part: this.partonomy.heads.values()) {
    		partonomy.add(part);
    	}
    	return partonomy;
    }
    
	/**
     * Create a new empty solution
     *
     * @param terms The terms included in the complete solution
     * @return An empty solution
     */
    public static KnowledgeGraphSolution empty(Set<String> terms) {
        return new KnowledgeGraphSolution(terms);
    }
    
    public KnowledgeGraphSolution clone() {    	
    	KnowledgeGraphSolution copy = new KnowledgeGraphSolution(new HashSet<String>(this.terms));
    	if (this.taxonomy != null) 
    		copy.taxonomy = new TaxonomySolution(new HashMap<String, Taxonomy>(this.taxonomy.heads), new HashSet<String>(this.terms));
    	else
    		copy.taxonomy = null;
    	
    	if (this.partonomy != null)
    		copy.partonomy = new TaxonomySolution(new HashMap<String, Taxonomy>(this.partonomy.heads), new HashSet<String>(this.terms));
    	else
    		copy.partonomy = null;
    	
    	return copy;
    }
    
    /**
     * Add a link to create a new partial solution
     *
     * @param top The top (broader) term
     * @param bottom The bottom (narrower) term
     * @param topScore The score of the top term
     * @param bottomScore The score of the bottom term
     * @param linkScore The link score
     * @param required Is this a mandatory link?
     * @return
     */
    public KnowledgeGraphSolution add(final TypedLink link,
                        final double topScore, final double bottomScore, 
                        final double linkScore,
                        final boolean required) {
    	
    	KnowledgeGraphSolution kgs = this.clone();
    	//FIXME Minimum linkscore should depend on how many relations there are
    	if (linkScore > 0.25) {
	    	switch(link.getType()) {
		    	case hypernymy:
		    		kgs.taxonomy = kgs.taxonomy.add(link.getSource(), link.getTarget(), topScore, bottomScore, linkScore, required);
		    		if (kgs.taxonomy == null)
		    			return null;
		    		break;
		    	case hyponymy:
		    		kgs.taxonomy = kgs.taxonomy.add(link.getTarget(), link.getSource(), bottomScore, topScore, linkScore, required);
		    		if (kgs.taxonomy == null)
		    			return null;
		    		break;
		    	case meronymy:
		    		kgs.partonomy = kgs.partonomy.add(link.getSource(), link.getTarget(), topScore, bottomScore, linkScore, required);
		    		if (kgs.partonomy == null)
		    			return null;
		    		break;
		    	default:
	    	}
    	}
		return kgs;
    }
    
    /**
     * *
     * Check if the solution has completed
     *
     * @return true if the solution is valid
     */
    //FIXME: The taxonomy should be complete and all links regarding the other relations should have been considered
    public boolean isComplete() {
        return this.taxonomy.isComplete();
    }
}
