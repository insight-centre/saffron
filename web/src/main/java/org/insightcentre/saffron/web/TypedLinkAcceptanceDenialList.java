package org.insightcentre.saffron.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.search.testing.KnowledgeGraph;

/**
 * Contains all the accepted and rejected suggestions packaged so that they can
 * be used in term/knowledge graph extraction.
 *
 * @author John McCrae
 * @author Bianca Pereira
 *
 */
public class TypedLinkAcceptanceDenialList extends AllowanceDenialList<TypedLink>{

		public TypedLinkAcceptanceDenialList(Set<String> termWhiteList, Set<String> termBlackList, Set<TypedLink> taxoWhiteList, Set<TypedLink> taxoBlackList) {
	        super(termWhiteList, termBlackList, taxoWhiteList, taxoBlackList);
	    }

	    public TypedLinkAcceptanceDenialList() {
	        super();
	    }

		protected static TypedLinkAcceptanceDenialList from(List<Term> allTerms, KnowledgeGraph kg) {

	        Set<String> termAllowanceList = new HashSet<>();
	        Set<String> termDenialList = new HashSet<>();

	        for (Term term : allTerms) {
	            if (term.getStatus().equals(Status.accepted)) {
	                termAllowanceList.add(term.getString());

	                if (term.getOriginalTerm() != null && !term.getString().equals(term.getOriginalTerm())) {
	                    termDenialList.add(term.getOriginalTerm());
	                }
	            } else if (term.getStatus().equals(Status.rejected)) {
	                termDenialList.add(term.getString());
	            }
	        }
	        termDenialList.removeAll(termAllowanceList);
	        
	        
	        return new TypedLinkAcceptanceDenialList(termAllowanceList, termDenialList,
	        		kg.getRelationsByStatus(Status.accepted), kg.getRelationsByStatus(Status.rejected));
	    }
}
