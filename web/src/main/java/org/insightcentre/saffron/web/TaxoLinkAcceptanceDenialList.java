package org.insightcentre.saffron.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.TaxoLink;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;

/**
 * Contains all the accepted and rejected suggestions packaged so that they can
 * be used in term/taxonomy extraction.
 *
 * @author John McCrae
 * @author Bianca Pereira
 *
 */
public class TaxoLinkAcceptanceDenialList extends AllowanceDenialList<TaxoLink>{

	    public TaxoLinkAcceptanceDenialList(Set<String> termAllowanceList, Set<String> termDenialList,
	    		Set<TaxoLink> taxoAllowanceList, Set<TaxoLink> taxoDenialList) {
	        super(termAllowanceList, termDenialList, taxoAllowanceList, taxoDenialList);
	    }

	    public TaxoLinkAcceptanceDenialList() {
	        super();
	    }

		protected static TaxoLinkAcceptanceDenialList from(List<Term> allTerms, Taxonomy taxonomy) {

	        Set<String> termWhiteList = new HashSet<>();
	        Set<String> termBlackList = new HashSet<>();

	        for (Term term : allTerms) {
	            if (term.getStatus().equals(Status.accepted)) {
	                termWhiteList.add(term.getString());

	                if (term.getOriginalTerm() != null && !term.getString().equals(term.getOriginalTerm())) {
	                    termBlackList.add(term.getOriginalTerm());
	                }
	            } else if (term.getStatus().equals(Status.rejected)) {
	                termBlackList.add(term.getString());
	            }
	        }

	        return new TaxoLinkAcceptanceDenialList(termWhiteList, termBlackList,
	        		taxonomy.getRelationsByStatus(Status.accepted), taxonomy.getRelationsByStatus(Status.rejected));
	    }
}
