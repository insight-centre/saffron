package org.insightcentre.nlp.saffron.taxonomy.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.metrics.SumKGScore;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;
import org.junit.Test;

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
     * Integration test
     * Test of extractTaxonomy method, of class GreedySplitTaxoExtract.
     * 
     * @author Andy Donald
     */
    @Test
    public void testExtractSynonymyNonNormalised() throws Exception {
        HashMap<String, Term> terms = new HashMap<>();
        addTerm(terms, "thing", 0.0);
        addTerm(terms, "vehicles", 0.0);
        addTerm(terms, "train", 0.0);
        addTerm(terms, "automobile", 0.0);
        addTerm(terms, "bus", 0.0);
        addTerm(terms, "coach", 0.0);
        addTerm(terms, "car", 0.0);
        addTerm(terms, "wheel", 0.0);
        addTerm(terms, "car wheel", 0.0);

		SumKGScore score = new SumKGScore(new TestMultiRelationClassifier(), false);
        GreedyKG instance = new GreedyKG(score, new KnowledgeGraphExtractionConfiguration(), new DefaultSaffronListener());
        KnowledgeGraph result = instance.extractKnowledgeGraph(terms);
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
		addTerm(terms, "thing", 0.0);
		addTerm(terms, "vehicles", 0.0);
		addTerm(terms, "train", 0.0);
		addTerm(terms, "automobile", 0.0);
		addTerm(terms, "bus", 0.0);
		addTerm(terms, "coach", 0.0);
		addTerm(terms, "car", 0.0);
		addTerm(terms, "wheel", 0.0);
		addTerm(terms, "car wheel", 0.0);

		SumKGScore score = new SumKGScore(new TestMultiRelationClassifier(), true);
		GreedyKG instance = new GreedyKG(score, new KnowledgeGraphExtractionConfiguration(), new DefaultSaffronListener());
		KnowledgeGraph result = instance.extractKnowledgeGraph(terms);
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
