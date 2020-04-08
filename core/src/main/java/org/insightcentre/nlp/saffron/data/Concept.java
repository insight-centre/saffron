package org.insightcentre.nlp.saffron.data;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A concept represented by one or more terms
 * 
 * @author Bianca Pereira
 *
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Concept {
	
	public final static String JSON_ID = "id";
	public final static String JSON_PREFERRED_TERM = "preferred_term";
	public final static String JSON_SYNONYMS = "synonyms";

	private String id;
	@JsonIgnore
	private Term preferredTerm;
	@JsonIgnore
	private Set<Term> synonyms;
	
	@JsonCreator
	public Concept(@JsonProperty(value = JSON_ID, required = true)String id, 
			@JsonProperty(value = JSON_PREFERRED_TERM, required = true) String preferredTerm,
			@JsonProperty(value = JSON_SYNONYMS) Set<String> synonyms) {
		this(id, new Term.Builder(preferredTerm).build());
		if (synonyms != null)
			this.setSynonymsFromStrings(synonyms);
	}
	
	public Concept(String id, Term preferredTerm) {
		this.setId(id);
		this.setPreferredTerm(preferredTerm);
		this.setSynonyms(new HashSet<Term>());
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Term getPreferredTerm() {
		return preferredTerm;
	}
	
	@JsonGetter(JSON_PREFERRED_TERM)
	public String getPreferredTermString() {
		return preferredTerm.getString();
	}

	public void setPreferredTerm(Term preferredTerm) {
		this.preferredTerm = preferredTerm;
	}

	public Set<Term> getSynonyms() {
		return synonyms;
	}
	
	@JsonGetter(JSON_SYNONYMS)
	public Set<String> getSynonymsStrings() {
		Set<String> synonyms = new HashSet<String>();
		for(Term term: this.getSynonyms()) {
			synonyms.add(term.getString());
		}
		return synonyms;
	}

	public void addSynonym(Term synonym) {
		this.synonyms.add(synonym);
	}
	
	public void setSynonyms(Set<Term> synonyms) {
		this.synonyms = synonyms;
	}
	
	public void setSynonymsFromStrings(Set<String> synonyms) {
		for(String term: synonyms) {
			this.addSynonym(new Term.Builder(term).build());
		}
	}

	@Override
	public String toString() {
		return "Concept [id=" + id + ", preferredTerm=" + preferredTerm + ", synonyms=" + synonyms + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((preferredTerm == null) ? 0 : preferredTerm.hashCode());
		result = prime * result + ((synonyms == null) ? 0 : synonyms.hashCode());
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
		Concept other = (Concept) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (preferredTerm == null) {
			if (other.preferredTerm != null)
				return false;
		} else if (!preferredTerm.equals(other.preferredTerm))
			return false;
		if (synonyms == null) {
			if (other.synonyms != null)
				return false;
		} else if (!synonyms.equals(other.synonyms))
			return false;
		return true;
	}	

	public static class Builder {
		
		private Concept concept;
		
		public Builder(String id, String preferredTerm) {
			this.concept = new Concept(id, new Term.Builder(preferredTerm).build());
		}
		
		public Builder id(String id) {
			this.concept.setId(id);
			return this;
		}
		
		public Builder preferredTerm(Term preferredTerm) {
			this.concept.setPreferredTerm(preferredTerm);
			return this;
		}
		
		public Builder preferredTerm(String preferredTerm) {
			this.concept.setPreferredTerm(new Term.Builder(preferredTerm).build());
			return this;
		}
		
		public Builder synonyms(Set<Term> synonyms) {
			this.concept.setSynonyms(synonyms);
			return this;
		}
		
		public Builder synonymString(Set<String> synonyms) {
			this.concept.setSynonymsFromStrings(synonyms);
			return this;
		}
		
		public Builder addSynonym(Term synonym) {
			if (this.concept.getSynonyms() == null)
				this.concept.setSynonyms(new HashSet<Term>());
			this.concept.addSynonym(synonym);
			return this;
		}
		
		public Builder addSynonym(String synonym) {
			if (this.concept.getSynonyms() == null)
				this.concept.setSynonyms(new HashSet<Term>());
			this.concept.addSynonym(new Term.Builder(synonym).build());
			return this;
		}
		
		public Concept build() {
			return this.concept;
		}
	}
}