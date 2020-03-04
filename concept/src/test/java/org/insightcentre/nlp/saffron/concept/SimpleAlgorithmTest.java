package org.insightcentre.nlp.saffron.concept;

import java.util.ArrayList;
import java.util.List;

import org.insightcentre.nlp.saffron.concept.consolidation.Simple;
import org.insightcentre.nlp.saffron.data.Concept;
import org.insightcentre.nlp.saffron.data.Term;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unitary tests for {@link Simple}
 * 
 * @author Bianca Pereira
 *
 */
public class SimpleAlgorithmTest {

	@Test
	public void testConsolidate() {
		//Prepare
		List<Concept> expected = new ArrayList<Concept>();
		expected.add(new Concept.Builder("term1", "term1").build());
		expected.add(new Concept.Builder("term2", "term2").build());
		expected.add(new Concept.Builder("term3", "term3").build());
		
		List<Term> input = new ArrayList<Term>();
		input.add(new Term.Builder("term2").occurrences(30).score(1.5).build());
		input.add(new Term.Builder("term1").occurrences(1).score(0.5).build());
		input.add(new Term.Builder("term3").occurrences(300).score(4.5).build());
		
		//Call
		List<Concept> actual = new Simple().consolidate(input);
		
		//Evaluate
		if(actual.size() > expected.size())
			fail("The number of concepts retrieved is higher than expected. expected=[" + expected.size() +"] but was [" + actual.size() + "]");
		else if(actual.size() < expected.size())
			fail("The number of concepts retrieved is lower than expected. expected=[" + expected.size() +"] but was [" + actual.size() + "]");
		
		for(Concept concept: actual) {
			if (!expected.contains(concept))
				fail(concept + " is not in the expected set");
		}
		for(Concept concept: expected) {
			if (!actual.contains(concept))
				fail(concept + " is expected but was not found");
		}
	}
	
	@Test
	public void testConsolidateDuplicatedTerms() {
		//Prepare
		List<Concept> expected = new ArrayList<Concept>();
		expected.add(new Concept.Builder("term1", "term1").build());
		expected.add(new Concept.Builder("term2", "term2").build());
		expected.add(new Concept.Builder("term3", "term3").build());
		
		List<Term> input = new ArrayList<Term>();
		input.add(new Term.Builder("term1").occurrences(1).score(0.5).build());
		input.add(new Term.Builder("term2").occurrences(30).score(1.5).build());
		input.add(new Term.Builder("term1").occurrences(1).score(0.5).build());
		input.add(new Term.Builder("term3").occurrences(300).score(4.5).build());
		
		//Call
		List<Concept> actual = new Simple().consolidate(input);
		
		//Evaluate
		if(actual.size() > expected.size())
			fail("The number of concepts retrieved is higher than expected. expected=[" + expected.size() +"] but was [" + actual.size() + "]");
		else if(actual.size() < expected.size())
			fail("The number of concepts retrieved is lower than expected. expected=[" + expected.size() +"] but was [" + actual.size() + "]");
		
		for(Concept concept: actual) {
			if (!expected.contains(concept))
				fail(concept + " is not in the expected set");
		}
		for(Concept concept: expected) {
			if (!actual.contains(concept))
				fail(concept + " is expected but was not found");
		}
	}

}
