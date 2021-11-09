package org.insightcentre.nlp.saffron.run;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.SaffronGraph;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;

/**
 * /**
 * Contains all the accepted and rejected suggestions packaged so that they can
 * be used in term/taxonomy/knowledge graph extraction.
 *
 * @author John McCrae
 * @author Bianca Pereira
 * @param <T> The type of the link
 *
 */
public class InclusionList<T extends TypedLink> {

    private Set<String> requiredTerms, excludedTerms;
    private Set<T> requiredRelations, excludedRelations;

    public InclusionList() {
        this.requiredTerms = new HashSet<>();
        this.excludedTerms = new HashSet<>();
        this.requiredRelations = new HashSet<>();
        this.excludedRelations = new HashSet<>();
    }

    public InclusionList(Set<String> requiredTerms, Set<String> excludedTerms,
            Set<T> requiredRelations, Set<T> excludedRelations) {
        this.requiredTerms = requiredTerms;
        this.excludedTerms = excludedTerms;
        this.requiredRelations = requiredRelations;
        this.excludedRelations = excludedRelations;
    }

    public Set<String> getRequiredTerms() {
        return requiredTerms;
    }

    public Set<String> getExcludedTerms() {
        return excludedTerms;
    }

    public Set<T> getRequiredRelations() {
        return requiredRelations;
    }

    public Set<T> getExcludedRelations() {
        return excludedRelations;
    }

    public void setRequiredTerms(Set<String> requiredTerms) {
        this.requiredTerms = requiredTerms;
    }

    public void setExcludedTerms(Set<String> excludedTerms) {
        this.excludedTerms = excludedTerms;
    }

    public void setRequiredRelations(Set<T> requiredRelations) {
        this.requiredRelations = requiredRelations;
    }

    public void setExcludedRelations(Set<T> excludedRelations) {
        this.excludedRelations = excludedRelations;
    }
    
    public static <T extends TypedLink> InclusionList<T> from(List<Term> allTerms, SaffronGraph<T> kg) {

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

        return new InclusionList(termAllowanceList, termDenialList,
                kg.getRelationsByStatus(Status.accepted), new HashSet<TypedLink>());
    }
}
