package org.insightcentre.nlp.saffron.data;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeGraph implements SaffronGraph<TypedLink> {

	private Taxonomy taxonomy;
	private Partonomy partonomy;
	private Collection<Set<String>> synonymyClusters;
	private Ontonomy ontonomy;

	public static KnowledgeGraph getEmptyInstance() {
		KnowledgeGraph kg = new KnowledgeGraph();
		kg.setTaxonomy(new Taxonomy("NO TERMS", 0, 0, Collections.EMPTY_LIST, Status.none));
		return kg;
	}
	
	public static KnowledgeGraph getSingleTermInstance(String term) {
		KnowledgeGraph kg = new KnowledgeGraph();
		kg.setTaxonomy(new Taxonomy(term, 0, 0, Collections.EMPTY_LIST, Status.none));
		return kg;
	}
	
	public Taxonomy getTaxonomy() {
		return taxonomy;
	}
	public void setTaxonomy(Taxonomy taxonomy) {
		this.taxonomy = taxonomy;
	}
	public Partonomy getPartonomy() {
		return partonomy;
	}
	public void setPartonomy(Partonomy partonomy) {
		this.partonomy = partonomy;
	}
	public Collection<Set<String>> getSynonymyClusters() {
		return synonymyClusters;
	}
	public void setSynonymyClusters(Collection<Set<String>> synonymyClusters) {
		this.synonymyClusters = synonymyClusters;
	}
	public Ontonomy getOntonomy() {
		return ontonomy;
	}
	public void setOntonomy(Ontonomy ontonomy) {
		this.ontonomy = ontonomy;
	}

	/**
     * Retrieve all synonymy relation pairs
     *
     * @return a {@link Set} with all synonymy relations
     *
     * @author Bianca Pereira
     */
    public Set<TypedLink> getSynonymyRelations() {
    	Set<TypedLink> relationPairs = new HashSet<TypedLink>();
    	for(Set<String> synonymCluster: synonymyClusters) {

    		Iterator<String> it = synonymCluster.iterator();

    		String targetSynonym;
    		if(it.hasNext()) {
    			targetSynonym = it.next();
    			while(it.hasNext()) {
    				relationPairs.add(new TypedLink(it.next(),targetSynonym,TypedLink.Type.synonymy));
    			}
    		}
    	}
    	return relationPairs;
    }

	/**
     * Retrieve all relation pairs with a given {@link Status}
     * 
     * @param status - the status of the pairs to be retrieved
     * @return a {@link Set} with all typed relations with that status
     * 
     * @author Bianca Pereira
     */
	public Set<TypedLink> getRelationsByStatus(Status status) {
		Set<TypedLink> result = new HashSet<TypedLink>();
		if (this.getSynonymyClusters() != null) {
			result.addAll(this.getSynonymyRelations());
		}
		if (this.getTaxonomy() != null) {
			result.addAll(this.getTaxonomy().getRelationsByStatus(status));
		}
		if (this.getPartonomy() != null) {
			result.addAll(this.getPartonomy().getRelationsByStatus(status));
		}
		if (this.getOntonomy() != null) {
			result.addAll(this.getOntonomy().getRelationsByStatus(status));
		}
		return result;
	}

	/**
	 * Build a KnowledgeGraph object from a string
	 * @param json file to read from
	 * @return a Taxonomy object
	 *
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static KnowledgeGraph fromJsonString(String json) throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(json, KnowledgeGraph.class);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ontonomy == null) ? 0 : ontonomy.hashCode());
		result = prime * result + ((partonomy == null) ? 0 : partonomy.hashCode());
		result = prime * result + ((taxonomy == null) ? 0 : taxonomy.hashCode());
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
		KnowledgeGraph other = (KnowledgeGraph) obj;
		if (ontonomy == null) {
			if (other.ontonomy != null)
				return false;
		} else if (!ontonomy.equals(other.ontonomy))
			return false;
		if (partonomy == null) {
			if (other.partonomy != null)
				return false;
		} else if (!partonomy.equals(other.partonomy))
			return false;
		if (taxonomy == null) {
			if (other.taxonomy != null)
				return false;
		} else if (!taxonomy.equals(other.taxonomy))
			return false;
		return true;
	}



	public static class Builder {

		KnowledgeGraph kg;

		public Builder() {
			kg = new KnowledgeGraph();
		}

		public KnowledgeGraph.Builder taxonomy(Taxonomy taxonomy) {
			kg.taxonomy = taxonomy;
			return this;
		}

		public KnowledgeGraph.Builder partonomy(Partonomy partonomy) {
			kg.partonomy = partonomy;
			return this;
		}

		public KnowledgeGraph.Builder ontonomy(Ontonomy ontonomy) {
			kg.ontonomy = ontonomy;
			return this;
		}

		public KnowledgeGraph.Builder synonoymyClusters(Collection<Set<String>> synonymyClusters) {
			kg.synonymyClusters = synonymyClusters;
			return this;
		}

		public KnowledgeGraph build() {
			return kg;
		}
	}
}
