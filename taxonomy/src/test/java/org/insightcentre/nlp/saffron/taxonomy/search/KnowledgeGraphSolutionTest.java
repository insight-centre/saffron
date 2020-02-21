package org.insightcentre.nlp.saffron.taxonomy.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.TypedLink;
import org.junit.Test;

public class KnowledgeGraphSolutionTest {


	@Test
	public void testAdd() {
		Set<String> terms = new HashSet<String>();
		terms.add("coach");
		terms.add("bus");
		terms.add("autobus");
		terms.add("automobile");
		
		//prepare
		KnowledgeGraphSolution kgs = new KnowledgeGraphSolution(terms);
		
		//test
		kgs = kgs.add(new TypedLink("coach","bus",TypedLink.Type.synonymy), 1.0, 1.0, 1.0, false);
		kgs = kgs.add(new TypedLink("bus","coach",TypedLink.Type.synonymy), 1.0, 1.0, 1.0, false);
		kgs = kgs.add(new TypedLink("autobus","coach",TypedLink.Type.synonymy), 1.0, 1.0, 1.0, false);
		kgs = kgs.add(new TypedLink("automobile","coach",TypedLink.Type.hypernymy), 1.0, 1.0, 1.0, false);
		
		//evaluate
		assertTrue(kgs.synonymyPairs.containsKey("coach"));
		assertTrue(kgs.synonymyPairs.containsKey("autobus"));
		assertFalse(kgs.synonymyPairs.containsKey("bus"));
		assertEquals("bus",kgs.synonymyPairs.get("coach"));
		assertEquals("bus",kgs.synonymyPairs.get("autobus"));
		assertTrue(kgs.taxonomy.heads.containsKey("automobile"));
		assertEquals("bus",kgs.taxonomy.heads.get("automobile").children.get(0).root);
	}

}
