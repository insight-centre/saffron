package org.insightcentre.saffron.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;

/**
 * /**
 * Contains all the accepted and rejected suggestions packaged so that they can
 * be used in term/taxonomy/knowledge graph extraction.
 *
 * @author John McCrae
 * @author Bianca Pereira
 *
 */
public abstract class AllowanceDenialList<T extends TypedLink> {

	private Set<String> termAllowanceList, termDenialList;
	private Set<T> relationAllowanceList, relationDenialList;    

	public AllowanceDenialList() {
		this.termAllowanceList = new HashSet<>();
		this.termDenialList = new HashSet<>();
		this.relationAllowanceList = new HashSet<>();
		this.relationDenialList = new HashSet<>();
	}
	
	public AllowanceDenialList(Set<String> termAllowanceList, Set<String> termDenialList, 
			Set<T> relationAllowanceList, Set<T> relationDenialList) {
		this.termAllowanceList = termAllowanceList;
		this.termDenialList = termDenialList;
		this.relationAllowanceList = relationAllowanceList;
		this.relationDenialList = relationDenialList;
	}
	
	public Set<String> getTermAllowanceList() {
		return termAllowanceList;
	}

	public Set<String> getTermDenialList() {
		return termDenialList;
	}

	public Set<T> getRelationAllowanceList() {
		return relationAllowanceList;
	}

	public Set<T> getRelationDenialList() {
		return relationDenialList;
	}

	public static AllowanceDenialList from(List<Term> allTerms, Object kg){
		if (kg instanceof Taxonomy) {
			return TaxoLinkAcceptanceDenialList.from(allTerms, (Taxonomy) kg);
		} else if (kg instanceof KnowledgeGraph) {
			return TypedLinkAcceptanceDenialList.from(allTerms, (KnowledgeGraph) kg);
		} else {
                    throw new RuntimeException("kg of unexpected type: " + kg.getClass().getName());
                }
	} 
	
	public static AllowanceDenialList getInstance(Class c) {
		if(c.equals(Taxonomy.class)) {
			return new TaxoLinkAcceptanceDenialList();
		} else {
			return new TypedLinkAcceptanceDenialList();
		}
	}
}
