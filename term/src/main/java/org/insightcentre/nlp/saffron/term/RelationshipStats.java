package org.insightcentre.nlp.saffron.term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.insightcentre.nlp.saffron.term.domain.DomainModelTermRelation;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Registers the frequency of the correlation between two strings
 *  
 * @author Bianca Pereira
 *
 */
public class RelationshipStats {

	private Map<String, Object2IntOpenHashMap<String>> relations;
	
	public RelationshipStats() {
		this.relations = new HashMap<String, Object2IntOpenHashMap<String>>();
	}
	
	public void add(RelationshipStats other) {
		for(String source: other.relations.keySet()) {
			for(Entry<String, Integer> entry: other.relations.get(source).entrySet()) {
				this.addRelation(source, entry.getKey(), entry.getValue());
			}
		}
	}
	
	public void addRelation(String source, String target) {
		this.addRelation(source, target, 1);
	}
	
	public void addRelation(String source, String target, int frequency) {
		Object2IntOpenHashMap<String> targets;
		if(this.relations.containsKey(source)) {
			targets = this.relations.get(source);
		} else {
			targets = new Object2IntOpenHashMap<String>();
		}
		
		targets.put(target, targets.getInt(target) + frequency);
		this.relations.put(source, targets);
	}
	
	public List<DomainModelTermRelation> getRelations() {
		List<DomainModelTermRelation> dmTermPairs = new ArrayList<DomainModelTermRelation>();
		for(String source: this.relations.keySet()) {
			for(Entry<String, Integer> entry: this.relations.get(source).entrySet()) {
				dmTermPairs.add(new DomainModelTermRelation(source, entry.getKey(), entry.getValue()));
			}
		}
		
		return dmTermPairs;
	}
	
	/**
     * Filter the correspondence between domain model terms and terms to consider only those 
     * related to a list of term strings.
     * 
     * @param terms - the terms used to filter the domain model-term correspondences
     * @return a list of domain model-term relations only for terms given as input
     */
	public List<DomainModelTermRelation> getRelationsToTerms(List<String> terms) {
		List<DomainModelTermRelation> dmTermPairs = new ArrayList<DomainModelTermRelation>();
		for(String source: this.relations.keySet()) {
			for(Entry<String, Integer> entry: this.relations.get(source).entrySet()) {
				if (terms.contains(entry.getKey())) {
					dmTermPairs.add(new DomainModelTermRelation(source, entry.getKey(), entry.getValue()));
				}
			}
		}
		
		return dmTermPairs;
	}

	public boolean isEmpty() {
		return this.relations.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((relations == null) ? 0 : relations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationshipStats other = (RelationshipStats) obj;
		if (relations == null) {
			if (other.relations != null)
				return false;
		} else if (!relations.equals(other.relations))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RelationshipStats [relations=" + relations + "]";
	}
}
