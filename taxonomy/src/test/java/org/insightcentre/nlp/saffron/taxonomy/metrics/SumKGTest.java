package org.insightcentre.nlp.saffron.taxonomy.metrics;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.search.GreedyKG;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;
import org.junit.Test;
import org.mockito.Mockito;

public class SumKGTest {
	
	private static class TestMultiRelationClassifier implements MulticlassRelationClassifier<String> {



        public TestMultiRelationClassifier() throws IOException {
        }

        @Override
        public Map<TypedLink.Type, Double> predict(String source, String target) {
        	Map<TypedLink.Type, Double> predictions = new HashMap<TypedLink.Type,Double>();
        	predictions.put(TypedLink.Type.hypernymy, 0.0);
    		predictions.put(TypedLink.Type.hyponymy, 0.0);
    		predictions.put(TypedLink.Type.meronymy, 0.0);
    		predictions.put(TypedLink.Type.synonymy, 0.0);
    		predictions.put(TypedLink.Type.other, 1.0);
        	
        	switch(source) {

        		case "coach":
        			if (target.equals("automobile")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			} else if (target.equals("bus")) {
        				predictions.put(TypedLink.Type.synonymy, 0.7);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
				case "bus":
					if (target.equals("automobile")) {
						predictions.put(TypedLink.Type.hyponymy, 1.0);
						predictions.put(TypedLink.Type.other, 0.0);
					} else if (target.equals("coach")) {
						predictions.put(TypedLink.Type.synonymy, 0.8);
						predictions.put(TypedLink.Type.other, 0.0);
					}
					break;

        	}

    		return predictions;
        }
        
    }
    
    private void addTerm(HashMap<String, Term> terms, String t, double score) {
        terms.put(t, new Term(t, 0, 0, score, Collections.EMPTY_LIST, Status.none.toString()));
    }
    
    /**
     * Test if scores are properly normalised when the inverse synonymy relation is not
     * in the score map.
     * 
     * @author Bianca Pereira
     */
    @Test
    public void testNormaliseSynonymyScoresTest() {
    	//prepare
    	MulticlassRelationClassifier<String> mockClassifier = 
    			Mockito.mock(MulticlassRelationClassifier.class);
    	
    	SumKGScore testObject = new SumKGScore(mockClassifier, true);
    	
    	TypedLink input = new TypedLink("coach","bus",TypedLink.Type.synonymy);
    	
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.hypernymy), 0.3);
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.hyponymy), 0.2);
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.meronymy), 0.05);
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.other), 0.05);
    	//call
    	Double actual = testObject.normaliseSynonymyScores(input, 0.4);
    	
    	//evaluate
    	assertEquals("The normalised score is incorrect",0.4, actual, 0.0001);
    	assertEquals("The link-scores map has a different number of items than expected",4,testObject.scores.size());
    	
    	assertEquals("The hypernymy score was modified",0.3,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.hypernymy)), 0.0001);
    	assertEquals("The hyponymy score was modified",0.2,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.hyponymy)), 0.0001);
    	assertEquals("The meronymy score was modified",0.05,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.meronymy)), 0.0001);
    	assertEquals("The other score was modified",0.05,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.other)), 0.0001);
    	
    }
    
    /**
     * Test if scores are properly normalised when the inverse synonymy relation is
     * in the score map already.
     * 
     * @author Bianca Pereira
     */
    @Test
    public void testNormaliseSynonymyScoresTest2() {
    	//prepare
    	MulticlassRelationClassifier<String> mockClassifier = 
    			Mockito.mock(MulticlassRelationClassifier.class);
    	
    	SumKGScore testObject = new SumKGScore(mockClassifier, true);
    	
    	TypedLink link = new TypedLink("coach","bus",TypedLink.Type.synonymy);
    	TypedLink input = new TypedLink("bus","coach",TypedLink.Type.synonymy);
    	
    	testObject.scores.put(link, 0.4);
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.hypernymy), 0.3);
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.hyponymy), 0.2);
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.meronymy), 0.05);
    	testObject.scores.put(new TypedLink("coach","bus",TypedLink.Type.other), 0.05);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hypernymy), 0.1);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hyponymy), 0.05);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.meronymy), 0.15);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.other), 0.1);
    	
    	//call
    	Double actual = testObject.normaliseSynonymyScores(input, 0.6);
    	
    	//evaluate
    	assertEquals("The normalised score is incorrect",0.5, actual, 0.0001);
    	assertEquals("The link-scores map has a different number of items than expected",9,testObject.scores.size());
    	assertEquals("The relation within the link-scores map was not normalised",0.5, testObject.scores.getDouble(link),0.0001);
    	
    	assertEquals("The hypernymy score was modified",0.3,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.hypernymy)), 0.0001);
    	assertEquals("The hyponymy score was modified",0.2,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.hyponymy)), 0.0001);
    	assertEquals("The meronymy score was modified",0.05,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.meronymy)), 0.0001);
    	assertEquals("The other score was modified",0.05,testObject.scores.getDouble(new TypedLink("coach","bus",TypedLink.Type.other)), 0.0001);
    	
    	assertEquals("The hypernymy score was modified",0.1,testObject.scores.getDouble(new TypedLink("bus","coach",TypedLink.Type.hypernymy)), 0.0001);
    	assertEquals("The hyponymy score was modified",0.05,testObject.scores.getDouble(new TypedLink("bus","coach",TypedLink.Type.hyponymy)), 0.0001);
    	assertEquals("The meronymy score was modified",0.15,testObject.scores.getDouble(new TypedLink("bus","coach",TypedLink.Type.meronymy)), 0.0001);
    	assertEquals("The other score was modified",0.1,testObject.scores.getDouble(new TypedLink("bus","coach",TypedLink.Type.other)), 0.0001);
    }
    
    /**
     * Test score without synonymy normalisation
     * for a synonymy link
     * 
     * @author Bianca Pereira
     */
    @Test
    public void deltaScoreWithoutNormalisationTest() {
    	//prepare
    	MulticlassRelationClassifier<String> mockClassifier = 
    			Mockito.mock(MulticlassRelationClassifier.class);
    	
    	SumKGScore testObject = new SumKGScore(mockClassifier, false);
    	
    	Map<TypedLink.Type, Double> predictions = new HashMap<TypedLink.Type,Double>();
    	predictions.put(TypedLink.Type.hypernymy, 0.3);
		predictions.put(TypedLink.Type.hyponymy, 0.2);
		predictions.put(TypedLink.Type.meronymy, 0.05);
		predictions.put(TypedLink.Type.synonymy, 0.4);
		predictions.put(TypedLink.Type.other, 0.05);
    	Mockito.when(mockClassifier.predict("coach", "bus")).thenReturn(predictions);
    	
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.synonymy), 0.6);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hypernymy), 0.1);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hyponymy), 0.05);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.meronymy), 0.15);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.other), 0.1);
    	
    	TypedLink input = new TypedLink("coach","bus", TypedLink.Type.synonymy);
    	
    	//call
    	double actual = testObject.deltaScore(input);
    	
    	//evaluate
    	assertEquals(0.4, actual, 0.0001);
    }
    
    /**
     * Test score without synonymy normalisation
     * for a non-synonymy link
     * 
     * @author Bianca Pereira
     */
    @Test
    public void deltaScoreWithoutNormalisationTest2() {
    	//prepare
    	MulticlassRelationClassifier<String> mockClassifier = 
    			Mockito.mock(MulticlassRelationClassifier.class);
    	
    	SumKGScore testObject = new SumKGScore(mockClassifier, false);
    	
    	Map<TypedLink.Type, Double> predictions = new HashMap<TypedLink.Type,Double>();
    	predictions.put(TypedLink.Type.hypernymy, 0.3);
		predictions.put(TypedLink.Type.hyponymy, 0.2);
		predictions.put(TypedLink.Type.meronymy, 0.05);
		predictions.put(TypedLink.Type.synonymy, 0.4);
		predictions.put(TypedLink.Type.other, 0.05);
    	Mockito.when(mockClassifier.predict("coach", "bus")).thenReturn(predictions);
    	
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.synonymy), 0.6);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hypernymy), 0.1);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hyponymy), 0.05);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.meronymy), 0.15);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.other), 0.1);
    	
    	TypedLink input = new TypedLink("coach","bus", TypedLink.Type.hypernymy);
    	
    	//call
    	double actual = testObject.deltaScore(input);
    	
    	//evaluate
    	assertEquals(0.3, actual, 0.0001);
    }
    
    /**
     * Test score with synonymy normalisation
     * for a synonymy link
     * 
     * @author Bianca Pereira
     */
    @Test
    public void deltaScoreWithNormalisationTest() {
    	//prepare
    	MulticlassRelationClassifier<String> mockClassifier = 
    			Mockito.mock(MulticlassRelationClassifier.class);
    	
    	SumKGScore testObject = new SumKGScore(mockClassifier, true);
    	
    	Map<TypedLink.Type, Double> predictions = new HashMap<TypedLink.Type,Double>();
    	predictions.put(TypedLink.Type.hypernymy, 0.3);
		predictions.put(TypedLink.Type.hyponymy, 0.2);
		predictions.put(TypedLink.Type.meronymy, 0.05);
		predictions.put(TypedLink.Type.synonymy, 0.4);
		predictions.put(TypedLink.Type.other, 0.05);
    	Mockito.when(mockClassifier.predict("coach", "bus")).thenReturn(predictions);
    	
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.synonymy), 0.6);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hypernymy), 0.1);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hyponymy), 0.05);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.meronymy), 0.15);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.other), 0.1);
    	
    	TypedLink input = new TypedLink("coach","bus", TypedLink.Type.synonymy);
    	
    	//call
    	double actual = testObject.deltaScore(input);
    	
    	//evaluate
    	assertEquals(0.5, actual, 0.0001);
    }
    
    /**
     * Test score with synonymy normalisation
     * for a non-synonymy link
     * 
     * @author Bianca Pereira
     */
    @Test
    public void deltaScoreWithNormalisationTest2() {
    	//prepare
    	MulticlassRelationClassifier<String> mockClassifier = 
    			Mockito.mock(MulticlassRelationClassifier.class);
    	
    	SumKGScore testObject = new SumKGScore(mockClassifier, true);
    	
    	Map<TypedLink.Type, Double> predictions = new HashMap<TypedLink.Type,Double>();
    	predictions.put(TypedLink.Type.hypernymy, 0.3);
		predictions.put(TypedLink.Type.hyponymy, 0.2);
		predictions.put(TypedLink.Type.meronymy, 0.05);
		predictions.put(TypedLink.Type.synonymy, 0.4);
		predictions.put(TypedLink.Type.other, 0.05);
    	Mockito.when(mockClassifier.predict("coach", "bus")).thenReturn(predictions);
    	
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.synonymy), 0.6);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hypernymy), 0.1);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.hyponymy), 0.05);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.meronymy), 0.15);
    	testObject.scores.put(new TypedLink("bus","coach",TypedLink.Type.other), 0.1);
    	
    	TypedLink input = new TypedLink("coach","bus", TypedLink.Type.hypernymy);
    	
    	//call
    	double actual = testObject.deltaScore(input);
    	
    	//evaluate
    	assertEquals(0.3, actual, 0.0001);
    }
    
    /**
     * Integration test
     * Test of extractTaxonomy method, of class GreedySplitTaxoExtract.
     * 
     * @author Andy Donald
     */
    @Test
    public void testExtractSynonymyNonNormalised() throws Exception {
        HashMap<String, Term> terms = new HashMap<>();
       // addTerm(terms, "thing", 0.0);
       // addTerm(terms, "vehicles", 0.0);
       // addTerm(terms, "train", 0.0);
        addTerm(terms, "automobile", 0.0);
        addTerm(terms, "bus", 0.0);
        addTerm(terms, "coach", 0.0);
       // addTerm(terms, "car", 0.0);
       // addTerm(terms, "wheel", 0.0);
       // addTerm(terms, "car wheel", 0.0);

        Set<TypedLink.Type> relationTypes = new HashSet<TypedLink.Type>();
        relationTypes.add(TypedLink.Type.hypernymy);
        relationTypes.add(TypedLink.Type.hyponymy);
        relationTypes.add(TypedLink.Type.meronymy);
        relationTypes.add(TypedLink.Type.synonymy);
        relationTypes.add(TypedLink.Type.other);

		SumKGScore score = new SumKGScore(new TestMultiRelationClassifier(), false);
        GreedyKG instance = new GreedyKG(score, new KnowledgeGraphExtractionConfiguration(), new DefaultSaffronListener());
        KnowledgeGraph result = instance.extractKnowledgeGraph(terms, relationTypes);
//        for ( Object2DoubleMap.Entry<TypedLink> obj : score.scores.object2DoubleEntrySet()) {
//            if (obj.getDoubleValue() != 0.0 && obj.getDoubleValue() != 1.0 ) {
//                System.out.println(obj.toString());
//
//            }
//
//        }
        for ( Map.Entry<TypedLink, Double> obj : score.scores.entrySet()) {
            if (obj.getKey().getType().equals(TypedLink.Type.synonymy) &&
                    (obj.getKey().getTarget().equals("bus")) &&
                    (obj.getKey().getSource().equals("coach"))) {
                assertEquals(new Double(0.7), obj.getValue());
                System.out.println(obj.toString());
            }
            if (obj.getKey().getType().equals(TypedLink.Type.synonymy) &&
                    (obj.getKey().getTarget().equals("coach")) &&
                    (obj.getKey().getSource().equals("bus"))) {
                assertEquals(new Double(0.8), obj.getValue());
                System.out.println(obj.toString());
            }
            if (obj.getKey().getType().equals(TypedLink.Type.hyponymy) &&
                    (obj.getKey().getTarget().equals("automobile")) &&
                    (obj.getKey().getSource().equals("bus"))) {
                assertEquals(new Double(1.0), obj.getValue());
                System.out.println(obj.toString());
            }
            if (obj.getKey().getType().equals(TypedLink.Type.hyponymy) &&
                    (obj.getKey().getTarget().equals("bus")) &&
                    (obj.getKey().getSource().equals("automobile"))) {
                assertEquals(new Double(0.0), obj.getValue());
                System.out.println(obj.toString());
            }

        }
    }

	/**
	 * Integration test
	 * Test of extractTaxonomy method, of class GreedySplitTaxoExtract.
	 * 
	 * @author Andy Donald
	 */
	@Test
	public void testExtractSynonymyNormalised() throws Exception {
		HashMap<String, Term> terms = new HashMap<>();
		//addTerm(terms, "thing", 0.0);
		//addTerm(terms, "vehicles", 0.0);
		//addTerm(terms, "train", 0.0);
		addTerm(terms, "automobile", 0.0);
		addTerm(terms, "bus", 0.0);
		addTerm(terms, "coach", 0.0);
		//addTerm(terms, "car", 0.0);
		//addTerm(terms, "wheel", 0.0);
		//addTerm(terms, "car wheel", 0.0);

        Set<TypedLink.Type> relationTypes = new HashSet<TypedLink.Type>();
        relationTypes.add(TypedLink.Type.hypernymy);
        relationTypes.add(TypedLink.Type.hyponymy);
        relationTypes.add(TypedLink.Type.meronymy);
        relationTypes.add(TypedLink.Type.synonymy);
        relationTypes.add(TypedLink.Type.other);

		SumKGScore score = new SumKGScore(new TestMultiRelationClassifier(), true);
		GreedyKG instance = new GreedyKG(score, new KnowledgeGraphExtractionConfiguration(), new DefaultSaffronListener());
		KnowledgeGraph result = instance.extractKnowledgeGraph(terms,relationTypes);
        for ( Map.Entry<TypedLink, Double> obj : score.scores.entrySet()) {
            if (obj.getKey().getType().equals(TypedLink.Type.synonymy) &&
                    (obj.getKey().getTarget().equals("bus")) &&
                    (obj.getKey().getSource().equals("coach"))) {
                assertEquals(new Double(0.75), obj.getValue());
                System.out.println(obj.toString());
            }
            if (obj.getKey().getType().equals(TypedLink.Type.synonymy) &&
                    (obj.getKey().getTarget().equals("coach")) &&
                    (obj.getKey().getSource().equals("bus"))) {
                assertEquals(new Double(0.75), obj.getValue());
                System.out.println(obj.toString());
            }
            if (obj.getKey().getType().equals(TypedLink.Type.hyponymy) &&
                    (obj.getKey().getTarget().equals("automobile")) &&
                    (obj.getKey().getSource().equals("bus"))) {
                    assertEquals(new Double(1.0), obj.getValue());
                System.out.println(obj.toString());
            }
            if (obj.getKey().getType().equals(TypedLink.Type.hyponymy) &&
                    (obj.getKey().getTarget().equals("bus")) &&
                    (obj.getKey().getSource().equals("automobile"))) {
                assertEquals(new Double(0.0), obj.getValue());
                System.out.println(obj.toString());
            }

        }


	}
}
