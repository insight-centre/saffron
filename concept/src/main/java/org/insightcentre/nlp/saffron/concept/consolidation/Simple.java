package org.insightcentre.nlp.saffron.concept.consolidation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Concept;
import org.insightcentre.nlp.saffron.data.Term;

/**
 * Creates one {@link Concept} for each {@link Term}.
 * Assumes monosemy and no synonymy
 *  
 * @author Bianca Pereira
 *
 */
public class Simple implements ConceptConsolidation{

	@Override
	public List<Concept> consolidate(List<Term> terms) {
		//throw new NotImplementedException("Simple.consolidate method not implemented");
		Set<Concept> concepts = new HashSet<Concept>();
		for(Term term: terms) {
			concepts.add(new Concept.Builder(term.getString(), term.getString()).build());
		}
		return new ArrayList<Concept>(concepts);
	}

}
