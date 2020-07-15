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

import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Partonomy;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TypedLink;

public class KnowledgeGraphSolution extends Solution{

	protected TaxonomySolution taxonomy;
	private TaxonomySolution partonomy;
    public Set<String> terms;
    protected Map<String,String> synonymyPairs;//change to SynonymySolution
    
    private double synonymyThreshold;
    private double meronomyThreshold;
	
    protected KnowledgeGraphSolution(Set<String> terms) {
        this(terms, 0.5, 0.25);
    }
    
    protected KnowledgeGraphSolution(Set<String> terms, double synonymyThreshold, double meronomyThreshold) {
    	this.taxonomy = TaxonomySolution.empty(terms);
        this.partonomy = TaxonomySolution.empty(terms);
        this.taxonomy = TaxonomySolution.empty(terms);
        this.synonymyPairs = new HashMap<String,String>();
        this.terms = terms;
        this.synonymyThreshold = synonymyThreshold;
        this.meronomyThreshold = meronomyThreshold;
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
    
    private Partonomy getPartonomy(){
    	List<Taxonomy> components = null;
    	if (this.partonomy.heads != null && !this.partonomy.heads.isEmpty())
    		components = new ArrayList<Taxonomy>(this.partonomy.heads.values());
    	else
    		components = new ArrayList<Taxonomy>();
		return new Partonomy(components); 
    	
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
	 * /**
     * Create a new empty solution
     *
     * @param terms The terms included in the complete solution
     * @param synonymyThreshold The minimum probability threshold for synonym pairs
	 * @param meronomyThreshold The minimum probability threshold for part/whole pairs
     * @return An empty solution
	 */
    public static KnowledgeGraphSolution empty(Set<String> terms, double synonymyThreshold, double meronomyThreshold) {
        return new KnowledgeGraphSolution(terms, synonymyThreshold, meronomyThreshold);
    }
    
    public KnowledgeGraphSolution clone() {
    	KnowledgeGraphSolution copy = new KnowledgeGraphSolution(new HashSet<String>(this.terms), this.synonymyThreshold, this.meronomyThreshold);
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
	    		if (linkScore > this.meronomyThreshold) {
		    		link = resolveSynonyms(linkToBeAdded);
		    		kgs.partonomy = kgs.partonomy.add(link.getTarget(), link.getSource(), topScore, bottomScore, linkScore, required);
		    		if (kgs.partonomy == null)
		    			return null;
	    		}
		    	break;
	    	case synonymy:
	    		if (linkScore > this.synonymyThreshold) {
		    		link = linkToBeAdded;
		    		String currentTarget = link.getTarget();
		    		while(kgs.synonymyPairs.containsKey(currentTarget)) {
		    			if(!kgs.synonymyPairs.get(currentTarget).equals(link.getSource())) {
		    				currentTarget = kgs.synonymyPairs.get(currentTarget);
		    			} else {
		    				return kgs;
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
	 * Remove candidates that will not be considered for a new partial solution due to logical constraints
	 * 
	 * @param candidates - the list of candidates to be pruned
	 * @param link - the link being included in the partial solution 
	 */
	public void pruneCandidateList(Collection<TypedLink> candidates, TypedLink link) {		
		candidates.remove(new TypedLink(link.getTarget(), link.getSource(), link.getType()));
		
		switch(link.getType()) {
			case hypernymy:
				candidates.remove(new TypedLink(link.getSource(), link.getTarget(), TypedLink.Type.hyponymy));
				candidates.remove(new TypedLink(link.getTarget(), link.getSource(), TypedLink.Type.hyponymy));
				break;
			case hyponymy:
				candidates.remove(new TypedLink(link.getSource(), link.getTarget(), TypedLink.Type.hypernymy));
				candidates.remove(new TypedLink(link.getTarget(), link.getSource(), TypedLink.Type.hypernymy));
				break;			
		}		
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
