package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.search.testing.KnowledgeGraph;

public class KnowledgeGraphSolution extends Solution{

	private TaxonomySolution taxonomy;
	private TaxonomySolution partonomy;
    public Set<String> terms;
    private Map<String,String> synonymyPairs;//change to SynonymySolution
	
    private KnowledgeGraphSolution(Set<String> terms) {
        this.taxonomy = TaxonomySolution.empty(terms);
        this.partonomy = TaxonomySolution.empty(terms);
        this.taxonomy = TaxonomySolution.empty(terms);
        this.synonymyPairs = new HashMap<String,String>();
        this.terms = terms;
    }
    
    public KnowledgeGraph getKnowledgeGraph() {
    	KnowledgeGraph kg = new KnowledgeGraph();
    	kg.setTaxonomy(this.getTaxonomy());
    	kg.setPartonomy(this.getPartonomy());
    	kg.setSynonymyClusters(generateSynonymyClusters());
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
    
    //TODO: Needs testing
    private Collection<Set<String>> generateSynonymyClusters() {
    	Map<String,Set<String>> clusters = new HashMap<String, Set<String>>();
    	for(Entry<String,String> entry: this.synonymyPairs.entrySet()) {
    		if(clusters.containsKey(entry.getValue())) {
    			clusters.get(entry.getValue()).add(entry.getKey());
    		} else {
    			clusters.put(entry.getValue(), new HashSet<String>(Arrays.asList(entry.getKey(),entry.getValue())));
    		}
    	}
    	return clusters.values();
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
    	
    	if (this.synonymyPairs != null)
    		copy.synonymyPairs = this.synonymyPairs.entrySet() 
                    .stream() 
                    .collect( 
                        Collectors 
                            .toMap(Map.Entry::getKey, 
                                   Map.Entry::getValue));
    	else
    		copy.synonymyPairs = null;
    	
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
    public KnowledgeGraphSolution add(final TypedLink linkToBeAdded,
                        final double topScore, final double bottomScore, 
                        final double linkScore,
                        final boolean required) {
    	
    	KnowledgeGraphSolution kgs = this.clone();
    	TypedLink link = null;
    	switch(linkToBeAdded.getType()) {
	    	case hypernymy:
	    		if (!this.taxonomy.isComplete()) {
		    		link = resolveSynonyms(linkToBeAdded);
		    		kgs.taxonomy = kgs.taxonomy.add(link.getSource(), link.getTarget(), topScore, bottomScore, linkScore, required);
		    		if (kgs.taxonomy == null)
		    			return null;
	    		}
	    		break;
	    	case hyponymy:
	    		if (!this.taxonomy.isComplete()) {
		    		link = resolveSynonyms(linkToBeAdded);
		    		kgs.taxonomy = kgs.taxonomy.add(link.getTarget(), link.getSource(), bottomScore, topScore, linkScore, required);
		    		if (kgs.taxonomy == null)
		    			return null;
	    		}
	    		break;
	    	case meronymy:
	    		if (linkScore > 0.25) {
		    		link = resolveSynonyms(linkToBeAdded);
		    		kgs.partonomy = kgs.partonomy.add(link.getTarget(), link.getSource(), topScore, bottomScore, linkScore, required);
		    		if (kgs.partonomy == null)
		    			return null;
	    		}
		    	break;
	    	case synonymy:
	    		if (linkScore > 0.5) {
		    		link = linkToBeAdded;
		    		String currentTarget = link.getTarget();
		    		while(kgs.synonymyPairs.containsKey(currentTarget)) {
		    			if(!kgs.synonymyPairs.get(currentTarget).equals(link.getSource())) {
		    				currentTarget = kgs.synonymyPairs.get(currentTarget);
		    			}
		    		}
		    		kgs.synonymyPairs.put(link.getSource(), currentTarget);
		    		kgs.terms.remove(link.getSource());
	    		}
	    	default:
    	}
		return kgs;
    }
    
    /**
     * Resolve the link by providing a common synonym for its source and target,
     * while keeping the same relation
     * 
     * @param original - the link to be resolved
     * @return a new {@link TypedLink} pointed to the "preferred" synonym
     */
    private TypedLink resolveSynonyms(TypedLink original) {
    	String source = original.getSource();
    	while(this.synonymyPairs.containsKey(source)) {
    		source = this.synonymyPairs.get(source);
    	}
    	
    	String target = original.getTarget();
    	while(this.synonymyPairs.containsKey(target)) {
    		target = this.synonymyPairs.get(target);
    	}
    	
    	return new TypedLink(source, target, original.getType());
    }
    
    /**
     * *
     * Check if the solution has completed
     *
     * @return true if the solution is valid
     */
    //FIXME: The taxonomy should be complete and all links regarding the other relations should have been considered
    public boolean isComplete() {
        return this.taxonomy.size() == this.terms.size();
    }
}
